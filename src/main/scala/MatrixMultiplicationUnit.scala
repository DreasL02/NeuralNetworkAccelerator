import chisel3._
import chisel3.util.log2Ceil
import systolic_array.SystolicArray
class MatrixMultiplicationUnit(w : Int = 8, dimension : Int = 4) extends Module{
  val io = IO(new Bundle {
    val loadInputs = Input(Bool())
    val loadWeights = Input(Bool())

    val inputs = Input(Vec(dimension, Vec(dimension, UInt(w.W))))
    val weights = Input(Vec(dimension, Vec(dimension, UInt(w.W))))

    val biases = Input(Vec(dimension, Vec(dimension, UInt(w.W))))

    val valid = Output(Bool())
    val result = Output(Vec(dimension, Vec(dimension, UInt(w.W))))

    val signed = Input(Bool())
    val fixedPoint = Input(UInt(log2Ceil(w).W))
  })

  def counter(max: UInt) = {
    val x = RegInit(0.asUInt(max.getWidth.W))
    x := Mux(x === max, 0.U, x + 1.U)
    x
  }

  val inputsBuffers = for (i <- 0 until dimension) yield {
    val buffer = Module(new Buffer(w, dimension, i))
    buffer
  }

  val weightsBuffers = for (i <- 0 until dimension) yield {
    val buffer = Module(new Buffer(w, dimension, i))
    buffer
  }

  for (i <- 0 until dimension) {
    inputsBuffers(i).io.load := io.loadInputs
    weightsBuffers(i).io.load := io.loadWeights

    inputsBuffers(i).io.data := io.inputs(i)
    weightsBuffers(i).io.data := io.weights(i)
  }

  val systolicArray = Module(new SystolicArray(w, dimension))
  for (i <- 0 until dimension) {
    systolicArray.io.a(i) := inputsBuffers(i).io.output
    systolicArray.io.b(i) := weightsBuffers(i).io.output
  }
  systolicArray.io.fixedPoint := io.fixedPoint

  val accumulator = Module(new Accumulator(w, dimension))
  accumulator.io.values := systolicArray.io.c
  accumulator.io.biases := io.biases

  val rectifier = Module(new Rectifier(w, dimension))
  rectifier.io.values := accumulator.io.result
  rectifier.io.signed := io.signed
  io.result := rectifier.io.result

  io.valid := counter((dimension*dimension).asUInt-1.U)
}
