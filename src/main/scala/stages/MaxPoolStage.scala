package stages

import chisel3._
import onnx.Operators.MaxPoolType
import operators.MaxPool4d

class MaxPoolStage(
                    w: Int = 8,
                    inputShape: (Int, Int, Int, Int) = (32, 32, 32, 32), // batch size, number of input channels, height, width
                    kernelShape: (Int, Int) = (2, 2),
                    pads: (Int, Int) = (0, 0),
                    strides: (Int, Int) = (2, 2),
                    signed: Boolean = true
                  ) extends Stage1(w, inputShape, w) {

  def this(maxPoolType: MaxPoolType) = this(maxPoolType.w, maxPoolType.inputDimensions, maxPoolType.kernelShape, maxPoolType.pads, maxPoolType.strides, maxPoolType.signed)

  val outputShape = (
    inputShape._1,
    inputShape._2,
    (inputShape._3 - kernelShape._1 + 2 * pads._1) / strides._1 + 1,
    (inputShape._4 - kernelShape._2 + 2 * pads._2) / strides._2 + 1
  )

  val maxPool = Module(new MaxPool4d(w, inputShape, kernelShape, pads, strides, signed))

  maxPool.io.inputChannel <> io.inputChannel
  io.outputChannel <> maxPool.io.outputChannel

  latency = 1
  dspUsage = 0
}
