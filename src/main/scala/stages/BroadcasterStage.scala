package stages

import chisel3._
import onnx.Operators.BroadcasterType
import operators.Broadcaster

class BroadcasterStage(
                        wIn: Int,
                        shapeIn: (Int, Int, Int, Int),
                        shapeTarget: (Int, Int, Int, Int),
                      )
  extends Stage1(wIn, shapeIn, wIn) {

  override lazy val shapeOut = shapeTarget

  def this(broadcasterType: BroadcasterType) = this(broadcasterType.w, broadcasterType.operandShape, broadcasterType.newShape)

  val broadcaster = Module(new Broadcaster(wIn, shapeIn, shapeOut))

  broadcaster.io.inputChannel <> io.inputChannel
  io.outputChannel <> broadcaster.io.outputChannel

  latency = 0
  dspUsage = 0
}
