import chisel3._
import chisel3.util.log2Ceil
import systolic_array.SystolicArray
class MatrixMultiplicationUnit(w : Int = 8, dimension : Int = 4) extends Module{
  def counter(max: UInt) = {
    val x = RegInit(0.asUInt(max.getWidth.W))
    x := Mux(x === max, 0.U, x + 1.U)
    x
  }

  val io = IO(new Bundle {
    val inputs_load = Input(Bool())
    val weights_load = Input(Bool())

    val inputs = Input(Vec(dimension, Vec(dimension, UInt(w.W))))
    val weights = Input(Vec(dimension, Vec(dimension, UInt(w.W))))

    val biases = Input(Vec(dimension, Vec(dimension, UInt(w.W))))

    val valid = Output(Bool())
    val result = Output(Vec(dimension, UInt(w.W)))

    val fixedPoint = Input(UInt(log2Ceil(w).W))
    val signed = Input(Bool())
  })

  val inputs_buffers = Vec(dimension, Module(new Buffer(w, dimension)))
  val weights_buffers = Vec(dimension, Module(new Buffer(w, dimension)))

  for (i <- 0 until dimension) {
    inputs_buffers(i).io.load := io.inputs_load
    weights_buffers(i).io.load := io.weights_load

    inputs_buffers(i).io.data := io.inputs(i)
    weights_buffers(i).io.data := io.weights(i)
  }

  val systolicArray = Module(new SystolicArray(w, dimension))
  for (i <- 0 until dimension) {
    systolicArray.io.a(i) := inputs_buffers(i).io.output
    systolicArray.io.b(i) := weights_buffers(i).io.output
  }
  systolicArray.io.fixedPoint := io.fixedPoint

  val accumulator = Module(new Accumulator(w, dimension))
  accumulator.io.values := systolicArray.io.c
  accumulator.io.biases := io.biases

  val rectifier = Module(new Rectifier(w, dimension))
  rectifier.io.values := accumulator.io.result
  rectifier.io.signed := io.signed
  io.result := rectifier.io.result

  io.valid := counter(dimension*dimension-1.U)
}
