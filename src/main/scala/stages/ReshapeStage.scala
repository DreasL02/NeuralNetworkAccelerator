package stages

import chisel3._
import onnx.Operators.ReshapeType
import operators.Reshape

class ReshapeStage(
                    w: Int,
                    shapeIn: (Int, Int, Int, Int),
                    shapeDimensions: (Int, Int, Int, Int),
                    shapeTarget: (Int, Int, Int, Int)
                  ) extends Stage1(w, shapeIn, w) {

  def this(reshapeType: ReshapeType) = this(reshapeType.w, reshapeType.inputDimensions, reshapeType.shapeDimensions, reshapeType.newDimensions)

  override lazy val shapeOut = shapeTarget

  val reshape = Module(new Reshape(w, shapeIn, shapeDimensions, shapeOut))

  reshape.io.inputChannel <> io.inputChannel
  io.outputChannel <> reshape.io.outputChannel

  latency = 0
  dspUsage = 0
}
