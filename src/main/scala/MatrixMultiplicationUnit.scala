import chisel3._
import chisel3.util.log2Ceil
import systolic_array.SystolicArray

class MatrixMultiplicationUnit(w: Int = 8, dimension: Int = 4) extends Module {
  val io = IO(new Bundle {
    val loadInputs = Input(Bool())
    val loadWeights = Input(Bool())
    val loadBiases = Input(Bool())

    val inputs = Input(Vec(dimension, Vec(dimension, UInt(w.W))))
    val weights = Input(Vec(dimension, Vec(dimension, UInt(w.W))))

    val biases = Input(Vec(dimension, Vec(dimension, UInt(w.W))))

    val valid = Output(Bool())
    val result = Output(Vec(dimension, Vec(dimension, UInt(w.W))))

    val signed = Input(Bool())
    val fixedPoint = Input(UInt(log2Ceil(w).W))

    val buffer1 = Output(Vec(dimension, UInt(w.W)))
    val buffer2 = Output(Vec(dimension, UInt(w.W)))
  })

  def timer(max: UInt, reset: Bool) = {
    val x = RegInit(0.asUInt(max.getWidth.W))
    when(reset) {
      x := 0.U
    }
    val done = x === max
    x := Mux(done, 0.U, x + 1.U)
    done
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

  val biases = VecInit.fill(dimension, dimension)(RegInit(0.U))
  for (i <- 0 until dimension) {
    for (j <- 0 until dimension) {
      when(io.loadBiases) {
        biases(i)(j) := io.biases(i)(j)
      }
    }
  }

  val accumulator = Module(new Accumulator(w, dimension))
  for (i <- 0 until dimension) {
    for (j <- 0 until dimension) {
      accumulator.io.values(i)(j) := systolicArray.io.c(j)(i)
    }
  }
  accumulator.io.biases := biases

  val rectifier = Module(new Rectifier(w, dimension))
  rectifier.io.values := accumulator.io.result
  rectifier.io.signed := io.signed

  io.result := rectifier.io.result
  io.valid := timer(dimension.U * dimension.U - 1.U, io.loadInputs || io.loadWeights)

  for (i <- 0 until dimension) {
    io.buffer1(i) := inputsBuffers(i).io.output
    io.buffer2(i) := weightsBuffers(i).io.output
  }
}
