package communication

package chisel.lib.uart

import chisel3._
import chisel3.util._

class BufferedUartTx(frequency: Int, baudRate: Int, bufferByteSize: Int = 1) extends Module {
  val bufferBitSize = bufferByteSize * 8

  val io = IO(new Bundle {
    val txd = Output(UInt(1.W))
    val channel = Flipped(DecoupledIO(Vec(bufferByteSize, UInt(8.W))))
  })

  // TODO: This is mostly an IO placeholder for now.
  // This should really be implemented by using the ByteBuffer and a shared UartTx.
}