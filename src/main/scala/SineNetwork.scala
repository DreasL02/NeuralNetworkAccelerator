import activation_functions.ReLU
import chisel3._
import chisel3.util.DecoupledIO
import scala_utils.DimensionManipulation._
import scala_utils.Optional._

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
    val inputChannel = Flipped(new DecoupledIO(Vec(1, Vec(1, UInt(wResult.W)))))
    val outputChannel = new DecoupledIO(Vec(1, Vec(1, UInt(wResult.W))))

    val debugMMU1Input = optional(enableDebuggingIO, Output(Vec(1, UInt(w.W))))
    val debugMMU2Input = optional(enableDebuggingIO, Output(Vec(1, UInt(w.W))))
    val debugMMU3Input = optional(enableDebuggingIO, Output(Vec(1, UInt(w.W))))

    val debugMMU1Weights = optional(enableDebuggingIO, Output(Vec(16, UInt(w.W))))
    val debugMMU2Weights = optional(enableDebuggingIO, Output(Vec(16, UInt(w.W))))
    val debugMMU3Weights = optional(enableDebuggingIO, Output(Vec(1, UInt(w.W))))

    val debugMMU1Result = optional(enableDebuggingIO, Output(Vec(1, Vec(16, UInt(wResult.W)))))
    val debugMMU2Result = optional(enableDebuggingIO, Output(Vec(1, Vec(16, UInt(wResult.W)))))
    val debugMMU3Result = optional(enableDebuggingIO, Output(Vec(1, Vec(1, UInt(wResult.W)))))

    val debugBias1Biases = optional(enableDebuggingIO, Output(Vec(1, Vec(16, UInt(wResult.W)))))
    val debugBias2Biases = optional(enableDebuggingIO, Output(Vec(1, Vec(16, UInt(wResult.W)))))
    val debugBias3Biases = optional(enableDebuggingIO, Output(Vec(1, Vec(1, UInt(wResult.W)))))

    val debugRounder1Input = optional(enableDebuggingIO, Output(Vec(1, Vec(1, UInt(wResult.W)))))
    val debugRounder2Input = optional(enableDebuggingIO, Output(Vec(1, Vec(16, UInt(wResult.W)))))
    val debugRounder3Input = optional(enableDebuggingIO, Output(Vec(1, Vec(16, UInt(wResult.W)))))

    val debugRounder1Output = optional(enableDebuggingIO, Output(Vec(1, Vec(1, UInt(w.W)))))
    val debugRounder2Output = optional(enableDebuggingIO, Output(Vec(1, Vec(16, UInt(w.W)))))
    val debugRounder3Output = optional(enableDebuggingIO, Output(Vec(1, Vec(16, UInt(w.W)))))

    val debugReLU1Input = optional(enableDebuggingIO, Output(Vec(1, Vec(16, UInt(wResult.W)))))
    val debugReLU2Input = optional(enableDebuggingIO, Output(Vec(1, Vec(16, UInt(wResult.W)))))

    val debugReLU1Output = optional(enableDebuggingIO, Output(Vec(1, Vec(16, UInt(wResult.W)))))
    val debugReLU2Output = optional(enableDebuggingIO, Output(Vec(1, Vec(16, UInt(wResult.W)))))
  })

  val weights1 = Module(new Initializer(w, 1, 16, initialWeightMemoryStates(0)))
  val weights2 = Module(new Initializer(w, 16, 16, initialWeightMemoryStates(1)))
  val weights3 = Module(new Initializer(w, 16, 1, initialWeightMemoryStates(2)))

  val biases1 = Module(new Initializer(wResult, 1, 16, initialBiasMemoryStates(0)))
  val biases2 = Module(new Initializer(wResult, 1, 16, initialBiasMemoryStates(1)))
  val biases3 = Module(new Initializer(wResult, 1, 1, initialBiasMemoryStates(2)))

  val rounder1 = Module(new Rounder(w, wResult, 1, 1, signed, fixedPoint))
  rounder1.io.inputChannel <> io.inputChannel

  val mmu1 = Module(new MatMul(w, wResult, 1, 16, 1, signed, enableDebuggingIO))
  mmu1.io.inputChannel <> rounder1.io.resultChannel
  mmu1.io.weightChannel <> weights1.io.outputChannel

  val bias1 = Module(new Add(wResult, 1, 16, enableDebuggingIO))
  bias1.io.inputChannel <> mmu1.io.resultChannel
  bias1.io.biasChannel <> biases1.io.outputChannel

  val relu1 = Module(new ReLU(wResult, 1, 16, enableDebuggingIO))
  relu1.io.inputChannel <> bias1.io.resultChannel

  val rounder2 = Module(new Rounder(w, wResult, 1, 16, signed, fixedPoint))
  rounder2.io.inputChannel <> relu1.io.resultChannel

  val mmu2 = Module(new MatMul(w, wResult, 1, 16, 16, signed, enableDebuggingIO))
  mmu2.io.inputChannel <> rounder2.io.resultChannel
  mmu2.io.weightChannel <> weights2.io.outputChannel

  val bias2 = Module(new Add(wResult, 1, 16, enableDebuggingIO))
  bias2.io.inputChannel <> mmu2.io.resultChannel
  bias2.io.biasChannel <> biases2.io.outputChannel

  val relu2 = Module(new ReLU(wResult, 1, 16, enableDebuggingIO))
  relu2.io.inputChannel <> bias2.io.resultChannel

  val rounder3 = Module(new Rounder(w, wResult, 1, 16, signed, fixedPoint))
  rounder3.io.inputChannel <> relu2.io.resultChannel

  val mmu3 = Module(new MatMul(w, wResult, 1, 1, 16, signed, enableDebuggingIO))
  mmu3.io.inputChannel <> rounder3.io.resultChannel
  mmu3.io.weightChannel <> weights3.io.outputChannel

  val bias3 = Module(new Add(wResult, 1, 1, enableDebuggingIO))
  bias3.io.inputChannel <> mmu3.io.resultChannel
  bias3.io.biasChannel <> biases3.io.outputChannel

  io.outputChannel <> bias3.io.resultChannel

  if (enableDebuggingIO) {
    io.debugMMU1Input.get := mmu1.io.debugInputs.get
    io.debugMMU2Input.get := mmu2.io.debugInputs.get
    io.debugMMU3Input.get := mmu3.io.debugInputs.get

    io.debugMMU1Weights.get := mmu1.io.debugWeights.get
    io.debugMMU2Weights.get := mmu2.io.debugWeights.get
    io.debugMMU3Weights.get := mmu3.io.debugWeights.get

    io.debugMMU1Result.get := mmu1.io.debugResults.get
    io.debugMMU2Result.get := mmu2.io.debugResults.get
    io.debugMMU3Result.get := mmu3.io.debugResults.get

    io.debugBias1Biases.get := bias1.io.debugBiases.get
    io.debugBias2Biases.get := bias2.io.debugBiases.get
    io.debugBias3Biases.get := bias3.io.debugBiases.get

    io.debugRounder1Input.get := rounder1.io.inputChannel.bits
    io.debugRounder2Input.get := rounder2.io.inputChannel.bits
    io.debugRounder3Input.get := rounder3.io.inputChannel.bits

    io.debugRounder1Output.get := rounder1.io.resultChannel.bits
    io.debugRounder2Output.get := rounder2.io.resultChannel.bits
    io.debugRounder3Output.get := rounder3.io.resultChannel.bits

    io.debugReLU1Input.get := relu1.io.inputChannel.bits
    io.debugReLU2Input.get := relu2.io.inputChannel.bits

    io.debugReLU1Output.get := relu1.io.resultChannel.bits
    io.debugReLU2Output.get := relu2.io.resultChannel.bits
  }


}
