import chisel3._
import chisel3.util.log2Ceil
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
    val inputs = Input(Vec(numberOfRows, Vec(commonDimension, UInt(w.W)))) // should only be used when load is true
    val weights = Input(Vec(commonDimension, Vec(numberOfColumns, UInt(w.W)))) // should only be used when load is true

    val result = Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))) // result of layer

    val debugInputs = optional(enableDebuggingIO, Output(Vec(numberOfRows, UInt(w.W))))
    val debugWeights = optional(enableDebuggingIO, Output(Vec(numberOfColumns, UInt(w.W))))
    val debugResults = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))))

    val valid = Output(Bool()) // indicates that the systolic array should be done
    val ready = Input(Bool()) // indicates that the systolic array is ready to receive new inputs
  })

  val config = "Pure"

  if (config == "Pure") {
    val matMul = Module(new PureMatrixMultiplication(w, wResult, numberOfRows, numberOfColumns, commonDimension, signed, enableDebuggingIO))
    matMul.io.inputs := io.inputs
    matMul.io.weights := io.weights
    matMul.io.ready := io.ready

    io.valid := matMul.io.valid
    io.result := matMul.io.result

    if (enableDebuggingIO) {
      // Inputs and weights are unavailable in the PureMatrixMultiplication module so 0s are assigned to the debug signals
      io.debugInputs.get := VecInit(Seq.fill(numberOfRows)(0.U(w.W)))
      io.debugWeights.get := VecInit(Seq.fill(numberOfColumns)(0.U(w.W)))
      io.debugResults.get := matMul.io.result
    }
  } else if (config == "SystolicArray") {
    val module = Module(new BufferedSystolicArray(w, wResult, numberOfRows, numberOfColumns, commonDimension, signed, enableDebuggingIO))
    module.io.inputs := io.inputs
    module.io.weights := transpose(io.weights)
    module.io.ready := io.ready

    io.valid := module.io.valid
    io.result := module.io.result

    if (enableDebuggingIO) {
      io.debugInputs.get := module.io.debugInputs.get
      io.debugWeights.get := module.io.debugWeights.get
      io.debugResults.get := module.io.debugSystolicArrayResults.get
    }
  }
}

