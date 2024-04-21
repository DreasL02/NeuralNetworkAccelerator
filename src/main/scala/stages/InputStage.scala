package stages

import onnx.Operators.InputType
import chisel3._

class InputStage(
                  wIn: Int,
                  wOut: Int,
                  inputShape: (Int, Int, Int, Int),
                  outputShape: (Int, Int, Int, Int),
                  implementation: InputImplementation,
                  baudRate: Int,
                  frequency: Int
                ) extends Stage1(wIn, inputShape, wOut) {

  override lazy val shapeOut = outputShape

  def this(inputType: InputType) = this(inputType.wIn, inputType.wOut, inputType.inputShape, inputType.outputShape, inputType.implementation, inputType.baudRate, inputType.frequency)

  if (implementation == InputImplementation.Uart) {
    // TODO: Implement UART input, for now raise an error
    assert(inputShape == (1, 1, 1, 1), "UART output only supports one input")
    assert(wIn == 1, "UART output only supports one bit-width input")

    throw new NotImplementedError("UART input not implemented")

    io.inputChannel.ready := true.B

    latency = 0
    dspUsage = 0
  } else {
    io.outputChannel <> io.inputChannel
    latency = 0
    dspUsage = 0
  }
}
