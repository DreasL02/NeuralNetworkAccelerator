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

  val CYCLES_UNTIL_VALID: Int = dimension * dimension - 1

  def timer(max: Int, reset: Bool) = {
    val x = RegInit(0.U(log2Ceil(max + 1).W))
    val done = x === max.U
    x := Mux(done || reset, 0.U, x + 1.U)
    done
  }

  val inputsBuffers = for (i <- 0 until dimension) yield {
    val buffer = Module(new ShiftedBuffer(w, dimension, i))
    buffer
  }

  val weightsBuffers = for (i <- 0 until dimension) yield {
    val buffer = Module(new ShiftedBuffer(w, dimension, i))
    buffer
  }

  for (i <- 0 until dimension) {
    inputsBuffers(i).io.load := io.load
    weightsBuffers(i).io.load := io.load

    inputsBuffers(i).io.data := io.inputs(i)
    weightsBuffers(i).io.data := io.weights(i)
  }

  val systolicArray = Module(new SystolicArray(w, dimension))
  for (i <- 0 until dimension) {
    systolicArray.io.a(i) := inputsBuffers(i).io.output
    systolicArray.io.b(i) := weightsBuffers(i).io.output
  }

  val fixedPointReg = RegInit(0.U(log2Ceil(w).W))
  when(io.load) {
    fixedPointReg := io.fixedPoint
  }

  systolicArray.io.fixedPoint := fixedPointReg
  systolicArray.io.clear := io.load

  // Addition of biases
  val accumulator = Module(new Accumulator(w, dimension))
  for (i <- 0 until dimension) {
    for (j <- 0 until dimension) {
      // values from systolic array
      accumulator.io.values(i)(j) := systolicArray.io.c(j)(i)

      // biases stored in regs
      val biasReg = RegInit(0.U(w.W))
      when(io.load) {
        biasReg := io.biases(i)(j)
      }
      accumulator.io.biases(i)(j) := biasReg
    }
  }

  val rectifier = Module(new Rectifier(w, dimension))
  rectifier.io.values := accumulator.io.result

  val signedReg = RegInit(false.B)
  when(io.load) {
    signedReg := io.signed
  }
  rectifier.io.signed := signedReg

  io.result := rectifier.io.result
  io.valid := timer(CYCLES_UNTIL_VALID, io.load)
}
