package stages

import onnx.Operators.MatMulType
import chisel3._

class MatMulStage(
                   wIn: Int,
                   shapeIn1: (Int, Int, Int, Int),
                   shapeIn2: (Int, Int, Int, Int),
                   wOut: Int,
                   signed: Boolean,
                   implementation: MatMulImplementation = MatMulImplementation.Direct // TODO: remove this default value
                 ) extends Stage2(wIn, shapeIn1, wIn, shapeIn2, wOut) {

  def this(matMulType: MatMulType) = this(matMulType.wOperands, matMulType.operandADimensions, matMulType.operandBDimensions, matMulType.wResult, matMulType.signed)

  override lazy val shapeOut = (
    shapeIn1._1,
    shapeIn1._2,
    shapeIn1._3,
    shapeIn2._4
  )

  // TODO: make it so it changes
  if (implementation == MatMulImplementation.Direct) {
    val matMul = Module(new operators.MatMul4d(wIn, wOut, shapeIn1, shapeIn2, signed))
    matMul.io.inputChannel <> io.input1Channel
    matMul.io.weightChannel <> io.input2Channel
    io.outputChannel <> matMul.io.outputChannel

    latency = 0 // TODO: calculate latency
    dspUsage = 0 // TODO: calculate DSP usage
  } else {
    val matMul = Module(new operators.MatMul4d(wIn, wOut, shapeIn1, shapeIn2, signed))
    matMul.io.inputChannel <> io.input1Channel
    matMul.io.weightChannel <> io.input2Channel
    io.outputChannel <> matMul.io.outputChannel

    latency = 0 // TODO: calculate latency
    dspUsage = shapeIn1._2 * shapeIn2._2 * shapeIn1._3 * shapeIn2._4
  }


}
