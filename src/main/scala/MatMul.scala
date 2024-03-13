import chisel3._
import chisel3.util.{DecoupledIO, log2Ceil}
import module_utils.ShiftedBuffer
import scala_utils.DimensionManipulation.{reverseRows, transpose}
import scala_utils.Optional.optional
import systolic_array.{BufferedSystolicArray, SystolicArray}

class MatMul(
              w: Int = 8,
              wResult: Int = 32,
              numberOfRows: Int = 4, // number of rows in the result matrix / number of rows in the first matrix
              numberOfColumns: Int = 4, // number of columns in the result matrix / number of columns in the second matrix
              commonDimension: Int = 4, // number of columns in the first matrix and number of rows in the second matrix
              signed: Boolean = true,
              enableDebuggingIO: Boolean = true
            ) extends Module {

  // Additional constructor to create a MatMul module from a MatMulType
  def this(matMulType: onnx.Operators.MatMulType, enableDebuggingIO: Boolean) = this(
    matMulType.wOperands,
    matMulType.wResult,
    matMulType.operandADimensions._1,
    matMulType.operandBDimensions._2,
    matMulType.operandADimensions._2,
    matMulType.signed,
    enableDebuggingIO
  )

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(numberOfRows, Vec(commonDimension, UInt(w.W)))))
    val weightChannel = Flipped(new DecoupledIO(Vec(commonDimension, Vec(numberOfColumns, UInt(w.W)))))

    val resultChannel = new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W))))

    val debugInputs = optional(enableDebuggingIO, Output(Vec(numberOfRows, UInt(w.W))))
    val debugWeights = optional(enableDebuggingIO, Output(Vec(numberOfColumns, UInt(w.W))))
    val debugResults = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))))
  })

  val config = "SystolicArray"

  if (config == "Pure") {
    val pure = Module(new PureMatrixMultiplication(w, wResult, numberOfRows, numberOfColumns, commonDimension, signed, enableDebuggingIO))
    pure.io.inputChannel <> io.inputChannel
    pure.io.weightChannel <> io.weightChannel
    pure.io.resultChannel <> io.resultChannel

    if (enableDebuggingIO) {
      // Inputs and weights are unavailable in the PureMatrixMultiplication module so 0s are assigned to the debug signals
      io.debugInputs.get := VecInit(Seq.fill(numberOfRows)(0.U(w.W)))
      io.debugWeights.get := VecInit(Seq.fill(numberOfColumns)(0.U(w.W)))
      io.debugResults.get := pure.io.resultChannel.bits
    }
  } else if (config == "SystolicArray") {
    val systolic = Module(new BufferedSystolicArray(w, wResult, numberOfRows, numberOfColumns, commonDimension, signed, enableDebuggingIO))
    systolic.io.inputChannel <> io.inputChannel
    systolic.io.weightChannel <> io.weightChannel
    systolic.io.resultChannel <> io.resultChannel

    if (enableDebuggingIO) {
      io.debugInputs.get := systolic.io.debugInputs.get
      io.debugWeights.get := systolic.io.debugWeights.get
      io.debugResults.get := systolic.io.debugSystolicArrayResults.get
    } else if (config == "OneAtATimeMatrixMultiplication") {

    }
  }
}

