package stages

import onnx.Operators.OutputType
import chisel3._
import communication.chisel.lib.uart.{BufferedUartRxForTestingOnly, BufferedUartTxForTestingOnly}
import module_utils.ByteIntoFlatVectorCollector
import operators.Reshape

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
    assert(outputShape == (1, 1, 1, 1), "UART output only supports one output")
    assert(wOut == 1, "UART output only supports one bit-width output")

    val totalElements = outputShape._1 * outputShape._2 * outputShape._3 * outputShape._4
    val outputBitWidth = totalElements * wOut

    val bytesRequired = (outputBitWidth / 8.0f).ceil.toInt
    val bufferedUartTx = Module(new BufferedUartTxForTestingOnly(frequency, baudRate, bytesRequired))

    bufferedUartTx.io.txd := io.inputChannel.bits(0)(0)(0)(0)
    io.inputChannel.ready := true.B
    bufferedUartTx.io.inputChannel.valid := io.inputChannel.valid

    // Convert the bytes into a flat vector of correct width numbers
    val byteConverter = Module(new ByteIntoFlatVectorCollector(bytesRequired, wOut))

    byteConverter.io.inputChannel <> bufferedUartTx.io.inputChannel

    val reshaper = Module(new Reshape(wOut, (1, 1, 1, bytesRequired), (1, 1, 1, 1), outputShape))

    reshaper.io.inputChannel <> byteConverter.io.outputChannel

    io.outputChannel <> reshaper.io.outputChannel

    io.outputChannel.ready := true.B

    latency = 0
    dspUsage = 0
  } else {
    io.outputChannel <> io.inputChannel
    latency = 0
    dspUsage = 0
  }
}