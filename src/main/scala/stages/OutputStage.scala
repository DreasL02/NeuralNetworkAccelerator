package stages

import onnx.Operators.OutputType
import chisel3._

class OutputStage(
                   wIn: Int,
                   wOut: Int,
                   inputShape: (Int, Int, Int, Int),
                   outputShape: (Int, Int, Int, Int),
                   implementation: OutputImplementation,
                   baudRate: Int,
                   frequency: Int
                 ) extends Stage1(wIn, inputShape, wOut) {

  def this(outputType: OutputType) = this(outputType.wIn, outputType.wOut, outputType.inputShape, outputType.outputShape, outputType.implementation, outputType.baudRate, outputType.frequency)

  override lazy val shapeOut = outputShape

  if (implementation == OutputImplementation.Uart) {
    // TODO: Implement UART output, for now raise an error
    assert(outputShape == (1, 1, 1, 1), "UART output only supports one output")
    assert(wOut == 1, "UART output only supports one bit-width output")

    throw new NotImplementedError("UART output not implemented")

    io.outputChannel.ready := true.B

    latency = 0
    dspUsage = 0
  } else {
    io.outputChannel <> io.inputChannel
    latency = 0
    dspUsage = 0
  }
}