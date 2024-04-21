package stages

import onnx.Operators.InputType

class InputStage(
                  w: Int,
                  shape: (Int, Int, Int, Int)
                ) extends Stage1(w, shape, w) {

  def this(inputType: InputType) = this(inputType.w, inputType.shape)

  io.outputChannel <> io.inputChannel

  latency = 0
  dspUsage = 0
}
