package communication

package chisel.lib.uart

import chisel3._
import chisel3.util._

// TODO: Implement this for testing purposes?
/*
class BufferedUartRx2(bufferByteSize: Int = 1) extends Module {

  val io = IO(new Bundle {
    val outputChannel = new DecoupledIO(Vec(bufferByteSize, UInt(8.W)))
  })

  val uartRx = Module(new Rx(100, 1))
}
 */