import activation_functions.ReLU
import chisel3._
import chisel3.util.DecoupledIO
import module_utils.Adders
import scala_utils.Optional.optional


class LayerCalculator(
                       w: Int = 8,
                       wResult: Int = 32,
                       numberOfRows: Int = 4, // number of rows in the result matrix
                       numberOfColumns: Int = 4, // number of columns in the result matrix
                       commonDimension: Int = 4, // number of columns in the first matrix and number of rows in the second matrix
                       signed: Boolean = true,
                       fixedPoint: Int = 0,
                       enableDebuggingIO: Boolean = true // enable debug signals for testing
                     ) extends Module {

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(numberOfRows, Vec(commonDimension, UInt(w.W)))))
    val weightChannel = Flipped(new DecoupledIO(Vec(commonDimension, Vec(numberOfColumns, UInt(w.W)))))

    val biasChannel = Flipped(new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))))

    val resultChannel = new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W))))

    val debugInputs = optional(enableDebuggingIO, Output(Vec(numberOfRows, UInt(w.W))))
    val debugWeights = optional(enableDebuggingIO, Output(Vec(numberOfColumns, UInt(w.W))))
    val debugMatMulResults = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))))
    val debugBiases = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))))
    val debugRounderInputs = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))))
    val debugReLUInputs = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))))
  })

  private val matMul = Module(new MatMul(w, wResult, numberOfRows, numberOfColumns, commonDimension, signed, enableDebuggingIO))
  matMul.io.inputChannel <> io.inputChannel
  matMul.io.weightChannel <> io.weightChannel

  if (enableDebuggingIO) {
    io.debugInputs.get := matMul.io.debugInputs.get
    io.debugWeights.get := matMul.io.debugWeights.get
    io.debugMatMulResults.get := matMul.io.debugResults.get
  }

  private val add = Module(new Add(wResult, numberOfRows, numberOfColumns, enableDebuggingIO))
  add.io.inputChannel <> matMul.io.resultChannel
  add.io.biasChannel <> io.biasChannel


  if (enableDebuggingIO) {
    io.debugBiases.get := add.io.debugBiases.get
    io.debugRounderInputs.get := add.io.resultChannel.bits
  }

  private val rounder = Module(new Rounder(w, wResult, numberOfRows, numberOfColumns, signed, fixedPoint))
  rounder.io.inputChannel <> add.io.resultChannel

  if (enableDebuggingIO) {
    io.debugReLUInputs.get := rounder.io.resultChannel.bits
  }

  // ReLU
  private val reLU = Module(new ReLU(w, numberOfRows, numberOfColumns, signed))
  reLU.io.inputChannel <> rounder.io.resultChannel

  // Result of computations
  io.resultChannel <> reLU.io.resultChannel
}
