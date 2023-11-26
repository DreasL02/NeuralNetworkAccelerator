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

  // TODO: look at perhaps disabling the rectifier and accumulator when not needed (i.e. when valid is false)
  // Save power (tm)

  val CYCLES_UNTIL_VALID: Int = dimension * dimension - 1

  def timer(max: Int, reset: Bool) = {
    val x = RegInit(0.U(log2Ceil(max + 1).W))
    when(reset) {
      x := 0.U
    }
    val done = x === max.U
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

  val fixedPoint = Wire(UInt(log2Ceil(w).W))
  val fixedPointReg = RegInit(0.U(log2Ceil(w).W))
  fixedPoint := fixedPointReg
  when(io.load) {
    fixedPoint := io.fixedPoint
    fixedPointReg := io.fixedPoint
  }

  systolicArray.io.fixedPoint := fixedPoint

  val biases = VecInit.fill(dimension, dimension)(RegInit(0.U))

  for (i <- 0 until dimension) {
    for (j <- 0 until dimension) {
      val biasValue = Wire(UInt(w.W))

      biasValue := 0.U // default value when not loading
      when(io.load) {
        biasValue := io.biases(i)(j) // load the bias value
      }
      biases(i)(j) := ShiftRegister(biasValue, CYCLES_UNTIL_VALID) //delay the bias value
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

  val signed = Wire(Bool())
  signed := false.B // default value when not loading
  when(io.load) {
    signed := io.signed // load the signed value
  }
  rectifier.io.signed := ShiftRegister(signed, CYCLES_UNTIL_VALID) //delay the signed value

  io.result := rectifier.io.result
  io.valid := timer(CYCLES_UNTIL_VALID, io.load)
}
