package stages

import chisel3._
import onnx.Operators.RounderType
import operators.Rounder4d

class RounderStage(
                    wBefore: Int,
                    wAfter: Int,
                    shape: (Int, Int, Int, Int),
                    signed: Boolean,
                    fixedPoint: Int
                  ) extends Stage1(wBefore, shape, wAfter) {

  def this(rounderType: RounderType) = this(rounderType.wOperands, rounderType.wResult, rounderType.shape, rounderType.signed, rounderType.fixedPoint)

  val rounder = Module(new Rounder4d(wBefore, wAfter, shape, signed, fixedPoint))

  rounder.io.inputChannel <> io.inputChannel
  io.outputChannel <> rounder.io.outputChannel

}
