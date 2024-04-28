package stages

import onnx.Operators.MatMulType
import chisel3._
import chisel3.util.log2Ceil
import operators.{BufferedSystolicArray4d, DirectMatMul4d}

class MatMulStage(
                   wIn: Int,
                   shapeIn1: (Int, Int, Int, Int),
                   shapeIn2: (Int, Int, Int, Int),
                   wOut: Int,
                   signed: Boolean,
                   implementation: MatMulImplementation
                 ) extends Stage2(wIn, shapeIn1, wIn, shapeIn2, wOut) {

  def this(matMulType: MatMulType) = this(matMulType.wOperands, matMulType.operandAShape, matMulType.operandBShape, matMulType.wResult, matMulType.signed, matMulType.implementation)

  override lazy val shapeOut = (
    shapeIn1._1,
    shapeIn1._2,
    shapeIn1._3,
    shapeIn2._4
  )

  if (implementation == MatMulImplementation.SystolicArray) {
    val matMul = Module(new BufferedSystolicArray4d(wIn, wOut, shapeIn1, shapeIn2, signed))
    matMul.io.inputChannel <> io.input1Channel
    matMul.io.weightChannel <> io.input2Channel
    io.outputChannel <> matMul.io.outputChannel

    latency = shapeIn1._3 + shapeIn2._4 + shapeIn2._3 - 2 + 1
    dspUsage = shapeIn1._2 * shapeIn1._2 * shapeIn1._3 * shapeIn2._4
  } else {
    val matMul = Module(new DirectMatMul4d(wIn, wOut, shapeIn1, shapeIn2, signed))
    matMul.io.inputChannel <> io.input1Channel
    matMul.io.weightChannel <> io.input2Channel
    io.outputChannel <> matMul.io.outputChannel

    latency = 1 + log2Ceil(shapeIn2._3) + 1
    dspUsage = shapeIn1._1 * shapeIn2._2 * shapeIn1._3 * shapeIn2._4 * shapeIn2._3
  }
}
