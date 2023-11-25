import chisel3._
import chisel3.util.{ShiftRegister, log2Ceil}
import systolic_array.SystolicArray

class LayerCalculator(w: Int = 8, dimension: Int = 4) extends Module {
  val io = IO(new Bundle {
    val load = Input(Bool())
    val inputs = Input(Vec(dimension, Vec(dimension, UInt(w.W))))
    val weights = Input(Vec(dimension, Vec(dimension, UInt(w.W))))

    val biases = Input(Vec(dimension, Vec(dimension, UInt(w.W))))

    val valid = Output(Bool())
    val result = Output(Vec(dimension, Vec(dimension, UInt(w.W))))

    val signed = Input(Bool())
    val fixedPoint = Input(UInt(log2Ceil(w).W))
  })

  // TODO: look at disabling the rectifier and accumulator when not needed (i.e. when valid is false)

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
  systolicArray.io.fixedPoint := io.fixedPoint

  val biases = VecInit.fill(dimension, dimension)(RegInit(0.U))

  for (i <- 0 until dimension) {
    for (j <- 0 until dimension) {
      val biasValue = Wire(UInt(w.W))

      biasValue := 0.U // default value when not loading
      when(io.load) {
        biasValue := io.biases(i)(j) // load the bias value
      }
      biases(i)(j) := ShiftRegister(biasValue, dimension * dimension - 1) //delay the bias value
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
  io.valid := timer(dimension.U * dimension.U - 1.U, io.load)
}
