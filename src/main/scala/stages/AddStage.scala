package stages

import chisel3._
import onnx.Operators.AddType
import operators.{Add4d}

class AddStage(
                w: Int,
                shape: (Int, Int, Int, Int),
              )
  extends Stage2(w, shape, w, shape, w) {

  def this(addType: AddType) = this(addType.w, addType.shape)

  val add = Module(new Add4d(w, shape))

  add.io.inputChannel <> io.input1Channel
  add.io.biasChannel <> io.input2Channel
  io.outputChannel <> add.io.outputChannel
}
