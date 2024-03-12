import activation_functions.ReLU
import chisel3._
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
    val ready = Input(Bool()) // load values
    val valid = Output(Bool()) // indicates that the systolic array should be done

    val inputs = Input(Vec(numberOfRows, Vec(commonDimension, UInt(w.W)))) // should only be used when load is true
    val weights = Input(Vec(commonDimension, Vec(numberOfColumns, UInt(w.W)))) // should only be used when load is true

    val biases = Input(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))) // should only be used when load is true

    val result = Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W)))) // result of layer

    val debugInputs = optional(enableDebuggingIO, Output(Vec(numberOfRows, UInt(w.W))))
    val debugWeights = optional(enableDebuggingIO, Output(Vec(numberOfColumns, UInt(w.W))))
    val debugMatMulResults = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))))
    val debugBiases = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))))
    val debugRounderInputs = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))))
    val debugReLUInputs = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))))
  })

  val matMul = Module(new MatMul(w, wResult, numberOfRows, numberOfColumns, commonDimension, signed, enableDebuggingIO))
  matMul.io.ready := io.ready
  matMul.io.inputs := io.inputs
  matMul.io.weights := io.weights

  if (enableDebuggingIO) {
    io.debugInputs.get := matMul.io.debugInputs.get
    io.debugWeights.get := matMul.io.debugWeights.get
    io.debugMatMulResults.get := matMul.io.debugResults.get
  }

  val bufferedBias = Module(new Add(wResult, numberOfRows, numberOfColumns, enableDebuggingIO))
  bufferedBias.io.input := matMul.io.result
  bufferedBias.io.biases := io.biases
  bufferedBias.io.ready := matMul.io.valid

  if (enableDebuggingIO) {
    io.debugBiases.get := bufferedBias.io.debugBiases.get
    io.debugRounderInputs.get := bufferedBias.io.result
  }

  val rounder = Module(new Rounder(w, wResult, numberOfRows, numberOfColumns, signed, fixedPoint))
  rounder.io.input := bufferedBias.io.result
  rounder.io.ready := bufferedBias.io.valid

  if (enableDebuggingIO) {
    io.debugReLUInputs.get := rounder.io.output
  }

  // ReLU
  val rectifier = Module(new ReLU(w, numberOfRows, numberOfColumns, signed))
  rectifier.io.input := rounder.io.output
  rectifier.io.ready := rounder.io.valid

  // Result of computations
  io.result := rectifier.io.result
  io.valid := rectifier.io.valid
}
