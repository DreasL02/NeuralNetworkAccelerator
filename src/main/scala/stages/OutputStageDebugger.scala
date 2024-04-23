package stages

import chisel3._
import chisel3.util.DecoupledIO
import communication.chisel.lib.uart.BufferedUartTxForTestingOnly
import module_utils.FlatVectorIntoBytesCollector
import operators.Reshape

class OutputStageDebugger(
                   wIn: Int,
                   wOut: Int,
                   inputShape: (Int, Int, Int, Int),
                   outputShape: (Int, Int, Int, Int),
                   baudRate: Int,
                   frequency: Int
                 ) extends Module {

    val io = IO(new Bundle {
      val inputChannel = Flipped(DecoupledIO(Vec(inputShape._1, Vec(inputShape._2, Vec(inputShape._3, Vec(inputShape._4, UInt(wIn.W)))))))
      val txdPin = Output(UInt(1.W))


      val reshaperInputChannelBits = Output(Vec(inputShape._1, Vec(inputShape._2, Vec(inputShape._3, Vec(inputShape._4, UInt(wIn.W))))))
      val reshaperInputChannelReady = Output(Bool())
      val reshaperInputChannelValid = Output(Bool())

      val reshaperOutputChannelBits = Output(Vec(outputShape._1, Vec(outputShape._2, Vec(outputShape._3, Vec(outputShape._4, UInt(wIn.W))))))
      val reshaperOutputChannelReady = Output(Bool())
      val reshaperOutputChannelValid = Output(Bool())

      val byteConverterInputChannelBits = Output(Vec(1, UInt(32.W)))
      val byteConverterInputChannelReady = Output(Bool())
      val byteConverterInputChannelValid = Output(Bool())

      val byteConverterOutputChannelBits = Output(Vec(4, UInt(8.W)))
      val byteConverterOutputChannelReady = Output(Bool())
      val byteConverterOutputChannelValid = Output(Bool())

      val uartTxInputChannelBits = Output(Vec(4, UInt(8.W)))
      val uartTxInputChannelReady = Output(Bool())
      val uartTxInputChannelValid = Output(Bool())
    })

    assert(outputShape == (1, 1, 1, 1), "UART output only supports one output")
    assert(wOut == 1, "UART output only supports one bit-width output")

    val totalElements = outputShape._1 * outputShape._2 * outputShape._3 * outputShape._4
    val outputBitWidth = totalElements * wIn

    val bytesRequired = (outputBitWidth / 8.0f).ceil.toInt

    val reshaper = Module(new Reshape(wIn, inputShape, (1, 1, 1, 1), (1, 1, 1, 1)))

    reshaper.io.shapeChannel.valid := true.B // ignore me
    reshaper.io.shapeChannel.bits(0)(0)(0)(0) := 0.U // ignore me

    reshaper.io.inputChannel <> io.inputChannel

    val byteConverter = Module(new FlatVectorIntoBytesCollector(wIn, 1))

    byteConverter.io.inputChannel.valid := reshaper.io.outputChannel.valid
    reshaper.io.outputChannel.ready := byteConverter.io.inputChannel.ready
    byteConverter.io.inputChannel.bits := reshaper.io.outputChannel.bits(0)(0)(0)

    val bufferedUartTx = Module(new BufferedUartTxForTestingOnly(frequency, baudRate, bytesRequired))

    io.txdPin := bufferedUartTx.io.txd

    bufferedUartTx.io.inputChannel <> byteConverter.io.outputChannel


    // debugging
    io.reshaperInputChannelBits := reshaper.io.inputChannel.bits
    io.reshaperInputChannelReady := reshaper.io.inputChannel.ready
    io.reshaperInputChannelValid := reshaper.io.inputChannel.valid

    io.reshaperOutputChannelBits := reshaper.io.outputChannel.bits
    io.reshaperOutputChannelReady := reshaper.io.outputChannel.ready
    io.reshaperOutputChannelValid := reshaper.io.outputChannel.valid

    io.byteConverterInputChannelBits := byteConverter.io.inputChannel.bits
    io.byteConverterInputChannelReady := byteConverter.io.inputChannel.ready
    io.byteConverterInputChannelValid := byteConverter.io.inputChannel.valid

    io.byteConverterOutputChannelBits := byteConverter.io.outputChannel.bits
    io.byteConverterOutputChannelReady := byteConverter.io.outputChannel.ready
    io.byteConverterOutputChannelValid := byteConverter.io.outputChannel.valid

    io.uartTxInputChannelBits := bufferedUartTx.io.inputChannel.bits
    io.uartTxInputChannelReady := bufferedUartTx.io.inputChannel.ready
    io.uartTxInputChannelValid := bufferedUartTx.io.inputChannel.valid

}