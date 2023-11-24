package communication

package chisel.lib.uart

import chisel3._
import chisel3.util._

class BufferedUartRx(frequency: Int, baudRate: Int, bufferByteSize: Int = 1) extends Module {
  val bufferBitSize = bufferByteSize * 8

  val io = IO(new Bundle {
    val rxd = Input(UInt(1.W))
    val channel = new DecoupledIO(Vec(bufferByteSize, UInt(8.W)))
    val uartRxValidDebug = Output(Bool())
    val uartDebugBitsReg = Output(UInt(8.W))
    val uartDebugCntReg = Output(UInt(20.W))
    val uartDebugBits = Output(UInt(8.W))
    val bufferCounter = Output(UInt(8.W))
  })

  // TODO: Should this be owned by the buffer?
  // Consider making this Module take a connection to an existing uart. Saves hardware by not duplicating the uart logic.
  val uartRx = Module(new Rx(frequency, baudRate))

  io.uartRxValidDebug := uartRx.io.channel.valid
  io.uartDebugBitsReg := uartRx.io.debugBitsReg
  io.uartDebugCntReg := uartRx.io.debugCntReg
  io.uartDebugBits := uartRx.io.channel.bits

  uartRx.io.rxd := io.rxd
  uartRx.io.channel.ready := false.B // TODO: This is dependent on validreg?

  val buffer = RegInit(VecInit(Seq.fill(bufferByteSize)(0.U(8.W))))

  io.channel.bits := buffer

  val counter = RegInit(0.U(log2Ceil(bufferByteSize + 1).W))
  io.bufferCounter := counter

  val validReg = RegInit(false.B)
  io.channel.valid := validReg

  when(uartRx.io.channel.valid) {
    uartRx.io.channel.ready := true.B
    buffer(counter) := uartRx.io.channel.bits
    counter := counter + 1.U
  }

  when(counter === bufferByteSize.U) {
    validReg := true.B
    counter := 0.U
  }

  when(io.channel.ready && validReg) {
    validReg := false.B
  }
}
