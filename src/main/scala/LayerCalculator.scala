import activation_functions.Rectifier
import chisel3._
import chisel3.util.{ShiftRegister, log2Ceil}
import systolic_array.SystolicArray
import utils.Optional.optional

class LayerCalculator(w: Int = 8, wStore: Int = 32, xDimension: Int = 4, yDimension: Int = 4, signed: Boolean = true, fixedPoint: Int = 0, enableDebuggingIO: Boolean = true // enable debug signals for testing
                     ) extends Module {
  val io = IO(new Bundle {
    val load = Input(Bool()) // load values

    val inputs = Input(Vec(xDimension, Vec(yDimension, UInt(w.W)))) // should only be used when load is true
    val weights = Input(Vec(xDimension, Vec(yDimension, UInt(w.W)))) // should only be used when load is true
    val biases = Input(Vec(xDimension, Vec(yDimension, UInt(wStore.W)))) // should only be used when load is true

    val valid = Output(Bool()) // indicates that the systolic array should be done
    val result = Output(Vec(xDimension, Vec(yDimension, UInt(w.W)))) // result of layer

    val debugInputs = optional(enableDebuggingIO, Output(Vec(xDimension, UInt(w.W))))
    val debugWeights = optional(enableDebuggingIO, Output(Vec(yDimension, UInt(w.W))))
    val debugSystolicArrayResults = optional(enableDebuggingIO, Output(Vec(xDimension, Vec(yDimension, UInt(wStore.W)))))
    val debugBiases = optional(enableDebuggingIO, Output(Vec(xDimension, Vec(yDimension, UInt(wStore.W)))))
    val debugRounderInputs = optional(enableDebuggingIO, Output(Vec(xDimension, Vec(yDimension, UInt(wStore.W)))))
    val debugReLUInputs = optional(enableDebuggingIO, Output(Vec(xDimension, Vec(yDimension, UInt(wStore.W)))))
  })


  val bufferedSystolicArray = Module(new BufferedSystolicArray(w, wStore, xDimension, yDimension, signed, enableDebuggingIO))
  bufferedSystolicArray.io.load := io.load
  bufferedSystolicArray.io.inputs := io.inputs
  bufferedSystolicArray.io.weights := io.weights
  io.valid := bufferedSystolicArray.io.valid

  if (enableDebuggingIO) {
    io.debugInputs.get := bufferedSystolicArray.io.debugInputs.get
    io.debugWeights.get := bufferedSystolicArray.io.debugWeights.get
  }


  // Addition of biases
  val accumulator = Module(new Accumulator(wStore, xDimension, yDimension))
  for (row <- 0 until xDimension) {
    for (column <- 0 until yDimension) {
      // Map results from systolic array in column-major order to accumulator in row-major order
      accumulator.io.values(row)(column) := bufferedSystolicArray.io.result(column)(row) //TODO: Handle this correctly in the systolic array

      // Continuously emit bias values
      val biasReg = RegInit(0.U(wStore.W))
      when(io.load) {
        biasReg := io.biases(row)(column) // replace bias value
      }

      accumulator.io.biases(row)(column) := biasReg
      if (enableDebuggingIO) {
        io.debugSystolicArrayResults.get(row)(column) := bufferedSystolicArray.io.result(column)(row)
        io.debugBiases.get(row)(column) := biasReg
      }
    }
  }


  if (enableDebuggingIO) {
    io.debugRounderInputs.get := accumulator.io.result
  }

  val rounder = Module(new Rounder(w, wStore, xDimension, yDimension, signed, fixedPoint))
  rounder.io.input := accumulator.io.result

  if (enableDebuggingIO) {
    io.debugReLUInputs.get := rounder.io.output
  }

  // ReLU
  val rectifier = Module(new Rectifier(w, xDimension, yDimension, signed))
  rectifier.io.values := rounder.io.output

  // Add quantization step here
  // val quantizer = Module(new Quantizer(w, dimension))
  // quantizer.io.values := rectifier.io.result
  // quantizer.io.fixedPoint := fixedPointTargetReg
  // quantizer.io.signed := signedTargetReg
  // io.result := quantizer.io.result

  // Result of computations
  io.result := rectifier.io.result

}
