package stages

import onnx.Operators.OutputType

class OutputStage(
                   w: Int,
                   shape: (Int, Int, Int, Int)
                 ) extends Stage1(w, shape, w) {

  def this(outputType: OutputType) = this(outputType.w, outputType.dimensions)

  io.outputChannel <> io.inputChannel

  latency = 0
  dspUsage = 0
}