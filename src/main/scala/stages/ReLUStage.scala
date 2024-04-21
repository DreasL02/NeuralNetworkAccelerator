package stages

import chisel3._
import onnx.Operators.ReluType
import operators.ReLU4d

class ReLUStage(
                 w: Int,
                 shape: (Int, Int, Int, Int),
                 signed: Boolean
               ) extends Stage1(w, shape, w) {

  def this(reluType: ReluType) = this(reluType.w, reluType.shape, reluType.signed)

  val relu = Module(new ReLU4d(w, shape, signed))

  relu.io.inputChannel <> io.inputChannel
  io.outputChannel <> relu.io.outputChannel

  latency = 1
  dspUsage = 0
}
