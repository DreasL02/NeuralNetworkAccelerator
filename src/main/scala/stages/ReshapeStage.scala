package stages

import chisel3._
import onnx.Operators.ReshapeType
import operators.Reshape

class ReshapeStage(
                    w: Int,
                    shapeIn: (Int, Int, Int, Int),
                    shapeDimensions: (Int, Int, Int, Int),
                    shapeTarget: (Int, Int, Int, Int)
                  ) extends Stage2(w, shapeIn, w, shapeDimensions, 1) {

  def this(reshapeType: ReshapeType) = this(reshapeType.w, reshapeType.inputShape, reshapeType.shapeShape, reshapeType.newShape)

  override lazy val shapeOut = shapeTarget

  val reshape = Module(new Reshape(w, shapeIn, shapeDimensions, shapeOut))

  reshape.io.inputChannel <> io.input1Channel
  reshape.io.shapeChannel <> io.input2Channel
  io.outputChannel <> reshape.io.outputChannel

  latency = 0
  dspUsage = 0
}
