package stages

import onnx.Operators.InputType
import chisel3._
import communication.chisel.lib.uart.BufferedUartRxForTestingOnly
import module_utils.ByteIntoFlatVectorCollector
import operators.Reshape

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
    assert(inputShape == (1, 1, 1, 1), "UART output only supports one input (the RXD pin)")
    assert(wIn == 1, "UART output only supports one bit-width input")

    val totalElements = outputShape._1 * outputShape._2 * outputShape._3 * outputShape._4
    val inputBitWidth = totalElements * wOut

    val bytesRequired = (inputBitWidth / 8.0f).ceil.toInt
    val bufferedUartRx = Module(new BufferedUartRxForTestingOnly(frequency, baudRate, bytesRequired))

    bufferedUartRx.io.rxd := io.inputChannel.bits(0)(0)(0)(0)
    io.inputChannel.ready := true.B
    bufferedUartRx.io.outputChannel.ready := io.outputChannel.ready

    // Convert the bytes into a flat vector of correct width numbers
    val byteConverter = Module(new ByteIntoFlatVectorCollector(bytesRequired, wOut))

    byteConverter.io.inputChannel <> bufferedUartRx.io.outputChannel

    val flatOutputShape = (1, 1, 1, outputShape._1 * outputShape._2 * outputShape._3 * outputShape._4)
    val reshaper = Module(new Reshape(wOut, flatOutputShape, (1, 1, 1, 1), outputShape))


    reshaper.io.inputChannel.valid := byteConverter.io.outputChannel.valid
    reshaper.io.shapeChannel.valid := true.B
    reshaper.io.shapeChannel.bits(0)(0)(0)(0) := 0.U


    byteConverter.io.outputChannel.ready := reshaper.io.inputChannel.ready

    reshaper.io.inputChannel.bits(0)(0)(0) := byteConverter.io.outputChannel.bits

    io.outputChannel <> reshaper.io.outputChannel
  } else {
    io.outputChannel <> io.inputChannel
  }
}
