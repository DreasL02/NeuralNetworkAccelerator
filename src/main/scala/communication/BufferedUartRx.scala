package communication

package chisel.lib.uart

import chisel3._
import chisel3.util._

// This Module only exists for testing purposes. It is not intended to be used in a real design.
// For real designs, instantiate a ByteBuffer and UartRx separately. This allows for Uart module reuse.
class BufferedUartRx(frequency: Int, baudRate: Int, bufferByteSize: Int) extends Module {
  val io = IO(new Bundle {
    val rxd = Input(UInt(1.W))
    val cts = Output(UInt(1.W)) // Clear To Send
    val outputChannel = new DecoupledIO(Vec(bufferByteSize, UInt(8.W)))
  })

  val uartRx = Module(new UartRx(frequency, baudRate))
  uartRx.io.rxd := io.rxd
  io.cts := uartRx.io.cts

  val byteBuffer = Module(new DeSerializingByteBuffer(bufferByteSize))

  byteBuffer.io.inputChannel <> uartRx.io.outputChannel
  io.outputChannel <> byteBuffer.io.outputChannel
}