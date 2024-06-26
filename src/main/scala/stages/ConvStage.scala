package stages

import chisel3._
import onnx.Operators.ConvType
import operators.{ConvDirect, ConvIm2Col}

class ConvStage(
                 wIn: Int,
                 wOut: Int,
                 shapeInput: (Int, Int, Int, Int),
                 shapeKernel: (Int, Int, Int, Int),
                 signed: Boolean,
                 strides: (Int, Int),
                 pads: (Int, Int),
                 implementation: ConvImplementation,
                 print: Boolean = false
               )
  extends Stage2(wIn, shapeInput, wIn, shapeKernel, wOut) {

  assert(shapeInput._2 == shapeKernel._2, "The second dimension of the input and kernel tensors must be the same")

  def this(convType: ConvType) = this(convType.w, convType.wResult, convType.inputShape, convType.kernelShape, convType.signed, convType.strides, convType.pads, convType.implementation)

  override lazy val shapeOut = (
    shapeInput._1, // batch size
    shapeKernel._1, // number of output channels
    (shapeInput._3 - shapeKernel._3 + 2 * pads._1) / strides._1 + 1, // height
    (shapeInput._4 - shapeKernel._4 + 2 * pads._2) / strides._2 + 1 // width
  )

  if (implementation == ConvImplementation.Im2Col) {
    val im2col = Module(new ConvIm2Col(wIn, wOut, shapeInput, shapeKernel, signed, strides, pads, print))
    im2col.io.inputChannel <> io.input1Channel
    im2col.io.kernelChannel <> io.input2Channel
    io.outputChannel <> im2col.io.outputChannel
  } else {
    val conv = Module(new ConvDirect(wIn, wOut, shapeInput, shapeKernel, signed, strides, pads, print))
    conv.io.inputChannel <> io.input1Channel
    conv.io.kernelChannel <> io.input2Channel
    io.outputChannel <> conv.io.outputChannel
  }
}


