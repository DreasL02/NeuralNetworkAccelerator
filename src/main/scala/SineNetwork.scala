import activation_functions.ReLU
import chisel3._
import memories.MatrixROM
import systolic_array.BufferedSystolicArray
import scala_utils.DimensionManipulation._

class SineNetwork(
                   val w: Int = 8,
                   val wResult: Int = 32,
                   val signed: Boolean = true,
                   val fixedPoint: Int = 0,
                   val initialWeightMemoryStates: Array[Array[Array[BigInt]]], // 3 entries of the following dimensions: (1x16), (16x16), (1x16)
                   val initialBiasMemoryStates: Array[Array[Array[BigInt]]], // 3 entries of the following dimensions: (1x16), (1x16), (1x1)
                   val enableDebuggingIO: Boolean = true // enable debug signals for testing
                 )
  extends Module {
  val io = IO(new Bundle {
    val load = Input(Bool()) // load values
    val input = Input(UInt(w.W))
    val output = Output(UInt(w.W))
  })

  // convert initialWeightMemoryStates to Vec[Vec[UInt]]
  val weights1: Vec[Vec[UInt]] = VecInit.fill(1, 16)(0.U(w.W))
  val weights2: Vec[Vec[UInt]] = VecInit.fill(16, 16)(0.U(w.W))
  val weights3: Vec[Vec[UInt]] = VecInit.fill(16, 1)(0.U(w.W))
  for (i <- 0 until 1) {
    for (j <- 0 until 16) {
      weights1(i)(j) := initialWeightMemoryStates(0)(i)(j).U
    }
  }
  for (i <- 0 until 16) {
    for (j <- 0 until 16) {
      weights2(i)(j) := initialWeightMemoryStates(1)(i)(j).U
    }
  }
  for (i <- 0 until 16) {
    for (j <- 0 until 1) {
      weights3(i)(j) := initialWeightMemoryStates(2)(i)(j).U
    }
  }

  // convert initialBiasMemoryStates to Vec[Vec[UInt]]
  val biases1: Vec[Vec[UInt]] = VecInit.fill(1, 16)(0.U(w.W))
  val biases2: Vec[Vec[UInt]] = VecInit.fill(1, 16)(0.U(w.W))
  val biases3: Vec[Vec[UInt]] = VecInit.fill(1, 1)(0.U(w.W))
  for (i <- 0 until 1) {
    for (j <- 0 until 16) {
      biases1(i)(j) := initialBiasMemoryStates(0)(i)(j).U
    }
  }
  for (i <- 0 until 1) {
    for (j <- 0 until 16) {
      biases2(i)(j) := initialBiasMemoryStates(1)(i)(j).U
    }
  }
  for (i <- 0 until 1) {
    for (j <- 0 until 1) {
      biases3(i)(j) := initialBiasMemoryStates(2)(i)(j).U
    }
  }

  val mmu1 = Module(new BufferedSystolicArray(w, wResult, 1, 16, 1, true, enableDebuggingIO))
  mmu1.io.load := io.load
  mmu1.io.inputs := VecInit(Seq.fill(1)(VecInit(Seq.fill(1)(io.input))))
  mmu1.io.weights := reverseRows(transpose(weights1))

  val bias1 = Module(new BufferedBias(wResult, 1, 16, enableDebuggingIO))
  bias1.io.input := mmu1.io.result
  bias1.io.biases := biases1
  bias1.io.load := io.load

  val rounder1 = Module(new Rounder(w, wResult, 1, 16, signed, fixedPoint))
  rounder1.io.input := bias1.io.result

  val relu1 = Module(new ReLU(w, 1, 16, enableDebuggingIO))
  relu1.io.input := rounder1.io.output

  val mmu2 = Module(new BufferedSystolicArray(w, wResult, 1, 16, 16, true, enableDebuggingIO))
  mmu2.io.load := io.load
  mmu2.io.inputs := reverseRows(relu1.io.result)
  mmu2.io.weights := reverseRows(transpose(weights2))

  val bias2 = Module(new BufferedBias(wResult, 1, 16, enableDebuggingIO))
  bias2.io.input := mmu2.io.result
  bias2.io.biases := biases2
  bias2.io.load := io.load

  val rounder2 = Module(new Rounder(w, wResult, 1, 16, signed, fixedPoint))
  rounder2.io.input := bias2.io.result

  val relu2 = Module(new ReLU(wResult, 1, 16, enableDebuggingIO))
  relu2.io.input := rounder2.io.output

  val mmu3 = Module(new BufferedSystolicArray(w, wResult, 1, 1, 16, true, enableDebuggingIO))
  mmu3.io.load := io.load
  mmu3.io.inputs := reverseRows(relu2.io.result)
  mmu3.io.weights := reverseRows(transpose(weights3))

  val bias3 = Module(new BufferedBias(wResult, 1, 1, enableDebuggingIO))
  bias3.io.input := mmu3.io.result
  bias3.io.biases := biases3
  bias3.io.load := io.load

  val rounder3 = Module(new Rounder(w, wResult, 1, 1, signed, fixedPoint))
  rounder3.io.input := bias3.io.result

  io.output := rounder3.io.output(0)(0)
}
