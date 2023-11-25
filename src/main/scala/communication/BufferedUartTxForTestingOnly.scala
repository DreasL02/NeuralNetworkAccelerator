package communication

package chisel.lib.uart

import chisel3._
import chisel3.util._

// This Module only exists for testing purposes. It is not intended to be used in a real design.
// For real designs, instantiate a ByteBuffer and UartTx separately. This allows for Uart module reuse.
class BufferedUartTxForTestingOnly(frequency: Int, baudRate: Int, bufferByteSize: Int) extends Module {
  val io = IO(new Bundle {
    val txd = Output(UInt(1.W))
    val inputChannel = Flipped(DecoupledIO(Vec(bufferByteSize, UInt(8.W))))
  })

  val uartTx = Module(new UartTx(frequency, baudRate))
  io.txd := uartTx.io.txd

  val byteBuffer = Module(new SerializingByteBuffer(bufferByteSize))

  uartTx.io.inputChannel <> byteBuffer.io.outputChannel
  io.inputChannel <> byteBuffer.io.inputChannel
}