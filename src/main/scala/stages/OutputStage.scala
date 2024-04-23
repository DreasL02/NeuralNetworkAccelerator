package stages

import onnx.Operators.OutputType
import chisel3._
import communication.chisel.lib.uart.{BufferedUartTxForTestingOnly}
import module_utils.{FlatVectorIntoBytesCollector}
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
    val outputBitWidth = totalElements * wIn

    val bytesRequired = (outputBitWidth / 8.0f).ceil.toInt

    val reshaper = Module(new Reshape(wOut, inputShape, (1, 1, 1, 1), (1, 1, 1, 1)))

    reshaper.io.shapeChannel.valid := true.B // ignore me
    reshaper.io.shapeChannel.bits(0)(0)(0)(0) := 0.U // ignore me

    reshaper.io.inputChannel <> io.inputChannel

    val byteConverter = Module(new FlatVectorIntoBytesCollector(wIn, 1))

    byteConverter.io.inputChannel.valid := reshaper.io.outputChannel.valid
    reshaper.io.outputChannel.ready := byteConverter.io.inputChannel.ready
    byteConverter.io.inputChannel.bits := reshaper.io.outputChannel.bits(0)(0)(0)

    val bufferedUartTx = Module(new BufferedUartTxForTestingOnly(frequency, baudRate, bytesRequired))

    io.outputChannel.bits(0)(0)(0)(0) := bufferedUartTx.io.txd
    io.outputChannel.valid := true.B

    println(s"OutputStage: bytesRequired: $bytesRequired")
    println(byteConverter.io.outputChannel.bits.length)
    println(bufferedUartTx.io.inputChannel.bits.length)

    bufferedUartTx.io.inputChannel <> byteConverter.io.outputChannel

    latency = 0
    dspUsage = 0
  } else {
    io.outputChannel <> io.inputChannel
    latency = 0
    dspUsage = 0
  }
}