import activation_functions.Rectifier
import chisel3._
import chisel3.util.{ShiftRegister, log2Ceil}
import systolic_array.SystolicArray
import utils.Optional.optional

class LayerCalculator(w: Int = 8, wStore: Int = 32, xDimension: Int = 4, yDimension: Int = 4, enableDebuggingIO: Boolean = true // enable debug signals for testing
                     ) extends Module {
  val io = IO(new Bundle {
    val load = Input(Bool()) // load values

    val inputs = Input(Vec(xDimension, Vec(yDimension, UInt(w.W)))) // should only be used when load is true
    val weights = Input(Vec(xDimension, Vec(yDimension, UInt(w.W)))) // should only be used when load is true
    val biases = Input(Vec(xDimension, Vec(yDimension, UInt(wStore.W)))) // should only be used when load is true
    val signed = Input(Bool()) // should only be used when load is true
    val fixedPoint = Input(UInt(log2Ceil(w).W)) // should only be used when load is true

    val valid = Output(Bool()) // indicates that the systolic array should be done
    val result = Output(Vec(xDimension, Vec(yDimension, UInt(w.W)))) // result of layer

    val debugInputs = optional(enableDebuggingIO, Output(Vec(xDimension, UInt(w.W))))
    val debugWeights = optional(enableDebuggingIO, Output(Vec(yDimension, UInt(w.W))))
    val debugSystolicArrayResults = optional(enableDebuggingIO, Output(Vec(xDimension, Vec(yDimension, UInt(wStore.W)))))
    val debugBiases = optional(enableDebuggingIO, Output(Vec(xDimension, Vec(yDimension, UInt(wStore.W)))))
    val debugRounderInputs = optional(enableDebuggingIO, Output(Vec(xDimension, Vec(yDimension, UInt(wStore.W)))))
    val debugReLUInputs = optional(enableDebuggingIO, Output(Vec(xDimension, Vec(yDimension, UInt(wStore.W)))))
  })

  val CYCLES_UNTIL_VALID: Int = xDimension * yDimension - 1 // number of cycles until the systolic array is done and the result is valid

  def timer(max: Int, reset: Bool) = { // timer that counts up to max and then resets, can also be reset manually by asserting reset
    val x = RegInit(0.U(log2Ceil(max + 1).W))
    val done = x === max.U // done when x reaches max
    x := Mux(done || reset, 0.U, x + 1.U) // reset when done or reset is asserted, otherwise increment
    done
  }

  //TODO check if correct
  val inputsBuffers = for (i <- 0 until yDimension) yield { // create array of buffers for inputs
    val buffer = Module(new ShiftedBuffer(w, yDimension, i)) // shift each buffer by i to create systolic effect
    buffer // return module
  }

  //TODO check if correct
  val weightsBuffers = for (i <- 0 until xDimension) yield { // create array of buffers for weights
    val buffer = Module(new ShiftedBuffer(w, xDimension, i)) // shift each buffer by i to create systolic effect
    buffer // return module
  }

  val systolicArray = Module(new SystolicArray(w, wStore, xDimension, yDimension))

  // Connect buffers to signals
  for (i <- 0 until yDimension) {
    inputsBuffers(i).io.load := io.load
    inputsBuffers(i).io.data := io.inputs(i)
    systolicArray.io.a(i) := inputsBuffers(i).io.output
    if (enableDebuggingIO) {
      io.debugInputs.get(i) := inputsBuffers(i).io.output
    }
  }

  for (i <- 0 until xDimension) {
    weightsBuffers(i).io.load := io.load
    weightsBuffers(i).io.data := io.weights(i)
    systolicArray.io.b(i) := weightsBuffers(i).io.output
    if (enableDebuggingIO) {
      io.debugWeights.get(i) := weightsBuffers(i).io.output
    }
  }

  // Continuously emit signed value
  val signedReg = RegInit(false.B)
  when(io.load) {
    signedReg := io.signed // replace signed value
  }

  // Continuously emit fixed point value
  val fixedPointReg = RegInit(0.U(log2Ceil(w).W))
  when(io.load) {
    fixedPointReg := io.fixedPoint // replace fixed point value
  }

  systolicArray.io.signed := signedReg // connect signed value
  systolicArray.io.clear := io.load // clear systolic array when load is asserted

  // Addition of biases
  val accumulator = Module(new Accumulator(wStore, xDimension, yDimension))
  for (row <- 0 until xDimension) {
    for (column <- 0 until yDimension) {
      // Map results from systolic array in column-major order to accumulator in row-major order
      accumulator.io.values(row)(column) := systolicArray.io.c(column)(row) //TODO: Handle this correctly in the systolic array

      // Continuously emit bias values
      val biasReg = RegInit(0.U(wStore.W))
      when(io.load) {
        biasReg := io.biases(row)(column) // replace bias value
      }

      accumulator.io.biases(row)(column) := biasReg
      if (enableDebuggingIO) {
        io.debugSystolicArrayResults.get(row)(column) := systolicArray.io.c(column)(row)
        io.debugBiases.get(row)(column) := biasReg
      }
    }
  }


  if (enableDebuggingIO) {
    io.debugRounderInputs.get := accumulator.io.result
  }

  val rounder = Module(new Rounder(w, wStore, xDimension, yDimension))
  rounder.io.fixedPoint := fixedPointReg
  rounder.io.signed := signedReg
  rounder.io.input := accumulator.io.result

  if (enableDebuggingIO) {
    io.debugReLUInputs.get := rounder.io.output
  }

  // ReLU
  val rectifier = Module(new Rectifier(w, xDimension, yDimension))
  rectifier.io.signed := signedReg
  rectifier.io.values := rounder.io.output

  // Add quantization step here
  // val quantizer = Module(new Quantizer(w, dimension))
  // quantizer.io.values := rectifier.io.result
  // quantizer.io.fixedPoint := fixedPointTargetReg
  // quantizer.io.signed := signedTargetReg
  // io.result := quantizer.io.result

  // Result of computations
  io.result := rectifier.io.result

  // Signal that the computation is valid
  io.valid := timer(CYCLES_UNTIL_VALID, io.load)
}
