package stages

import chisel3._
import onnx.Operators.ConvType
import operators.{Conv4d, Conv4dMatmul}

class ConvStage(
                 wIn: Int = 8, // the width of the input tensor
                 wOut: Int = 32, // the width of the output tensor
                 shapeInput: (Int, Int, Int, Int) = (32, 32, 32, 32), // batch size, number of input channels, height, width
                 shapeKernel: (Int, Int, Int, Int) = (3, 3, 3, 3), // number of output channels, number of input channels, height, width
                 signed: Boolean = true, // whether the input and kernel tensors are signed
                 strides: (Int, Int) = (1, 1), // the stride to use for the convolution
                 pads: (Int, Int) = (0, 0), // the padding to use for the convolution
                 implementation: ConvImplementation = ConvImplementation.Im2Col,
                 print: Boolean = false
               )
  extends Stage2(wIn, shapeInput, wIn, shapeKernel, wOut) {

  assert(shapeInput._2 == shapeKernel._2, "The second dimension of the input and kernel tensors must be the same")

  def this(convType: ConvType) = this(convType.w, convType.wResult, convType.inputDimensions, convType.kernelDimensions, convType.signed, convType.strides, convType.pads)

  override lazy val shapeOut = (
    shapeInput._1, // batch size
    shapeKernel._1, // number of output channels
    (shapeInput._3 - shapeKernel._3 + 2 * pads._1) / strides._1 + 1, // height
    (shapeInput._4 - shapeKernel._4 + 2 * pads._2) / strides._2 + 1 // width
  )

  if (implementation == ConvImplementation.Im2Col) {
    val im2col = Module(new Conv4dMatmul(wIn, wOut, shapeInput, shapeKernel, signed, strides, pads, print))
    im2col.io.inputChannel <> io.input1Channel
    im2col.io.kernelChannel <> io.input2Channel
    io.outputChannel <> im2col.io.outputChannel

    latency = 0 // TODO: calculate latency
    dspUsage = 0 // TODO: calculate DSP usage
  } else {
    val conv = Module(new Conv4d(wIn, wOut, shapeInput, shapeKernel, signed, strides, pads, print))
    conv.io.inputChannel <> io.input1Channel
    conv.io.kernelChannel <> io.input2Channel
    io.outputChannel <> conv.io.outputChannel

    latency = 0 // TODO: calculate latency
    dspUsage = 0 // TODO: calculate DSP usage
  }


}

