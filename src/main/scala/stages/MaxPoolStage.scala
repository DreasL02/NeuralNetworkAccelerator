package stages

import chisel3._
import onnx.Operators.MaxPoolType
import operators.MaxPool4d

class MaxPoolStage(
                    w: Int,
                    inputShape: (Int, Int, Int, Int), // batch size, number of input channels, height, width
                    kernelShape: (Int, Int),
                    pads: (Int, Int),
                    strides: (Int, Int),
                    signed: Boolean
                  ) extends Stage1(w, inputShape, w) {

  def this(maxPoolType: MaxPoolType) = this(maxPoolType.w, maxPoolType.inputShape, maxPoolType.kernelShape, maxPoolType.pads, maxPoolType.strides, maxPoolType.signed)

  override lazy val shapeOut = (
    inputShape._1,
    inputShape._2,
    (inputShape._3 - kernelShape._1 + 2 * pads._1) / strides._1 + 1,
    (inputShape._4 - kernelShape._2 + 2 * pads._2) / strides._2 + 1
  )

  val maxPool = Module(new MaxPool4d(w, inputShape, kernelShape, pads, strides, signed))

  maxPool.io.inputChannel <> io.inputChannel
  io.outputChannel <> maxPool.io.outputChannel

}
