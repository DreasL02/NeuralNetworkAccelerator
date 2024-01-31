import chisel3._
import chisel3.util.{ShiftRegister, log2Ceil}
import systolic_array.SystolicArray

class LayerCalculator(w: Int = 8, dimension: Int = 4) extends Module {
  val io = IO(new Bundle {
    val load = Input(Bool()) // load values

    val inputs = Input(Vec(dimension, Vec(dimension, UInt(w.W)))) // should only be used when load is true
    val weights = Input(Vec(dimension, Vec(dimension, UInt(w.W)))) // should only be used when load is true
    val biases = Input(Vec(dimension, Vec(dimension, UInt(w.W)))) // should only be used when load is true
    val signed = Input(Bool()) // should only be used when load is true
    val fixedPoint = Input(UInt(log2Ceil(w).W)) // should only be used when load is true

    val valid = Output(Bool()) // indicates that the systolic array should be done
    val result = Output(Vec(dimension, Vec(dimension, UInt(w.W)))) // result of layer
  })

  val CYCLES_UNTIL_VALID: Int = dimension * dimension - 1 // number of cycles until the systolic array is done and the result is valid

  def timer(max: Int, reset: Bool) = { // timer that counts up to max and then resets, can also be reset manually by asserting reset
    val x = RegInit(0.U(log2Ceil(max + 1).W))
    val done = x === max.U // done when x reaches max
    x := Mux(done || reset, 0.U, x + 1.U) // reset when done or reset is asserted, otherwise increment
    done
  }

  val inputsBuffers = for (i <- 0 until dimension) yield { // create array of buffers for inputs
    val buffer = Module(new ShiftedBuffer(w, dimension, i)) // shift each buffer by i to create systolic effect
    buffer // return module
  }

  val weightsBuffers = for (i <- 0 until dimension) yield { // create array of buffers for weights
    val buffer = Module(new ShiftedBuffer(w, dimension, i)) // shift each buffer by i to create systolic effect
    buffer // return module
  }

  val systolicArray = Module(new SystolicArray(w, dimension))

  // Connect buffers to signals
  for (i <- 0 until dimension) {
    inputsBuffers(i).io.load := io.load
    weightsBuffers(i).io.load := io.load

    inputsBuffers(i).io.data := io.inputs(i)
    weightsBuffers(i).io.data := io.weights(i)

    systolicArray.io.a(i) := inputsBuffers(i).io.output
    systolicArray.io.b(i) := weightsBuffers(i).io.output
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

  systolicArray.io.fixedPoint := fixedPointReg // connect fixed point value
  systolicArray.io.signed := signedReg // connect signed value
  systolicArray.io.clear := io.load // clear systolic array when load is asserted

  // Addition of biases
  val accumulator = Module(new Accumulator(w, dimension))
  for (i <- 0 until dimension) {
    for (j <- 0 until dimension) {
      // Map results from systolic array in column-major order to accumulator in row-major order
      accumulator.io.values(i)(j) := systolicArray.io.c(j)(i)

      // Continuously emit bias values
      val biasReg = RegInit(0.U(w.W))
      when(io.load) {
        biasReg := io.biases(i)(j) // replace bias value
      }
      accumulator.io.biases(i)(j) := biasReg
    }
  }

  // ReLU
  val rectifier = Module(new Rectifier(w, dimension))
  rectifier.io.values := accumulator.io.result // connect accumulator output to rectifier input
  rectifier.io.signed := signedReg

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
