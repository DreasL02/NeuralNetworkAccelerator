package communication

package chisel.lib.uart

import chisel3._
import chisel3.util._

class ByteBuffer(bufferByteSize: Int) extends Module {
  val bufferBitSize = bufferByteSize * 8

  val io = IO(new Bundle {
    val outputChannel = new DecoupledIO(Vec(bufferByteSize, UInt(8.W)))
    val inputChannel = Flipped(new DecoupledIO(UInt(8.W)))
  })

  // The input channel is always ready, as we only take one cycle to buffer a byte.
  io.inputChannel.ready := true.B

  val byteBufferValidReg = RegInit(false.B)
  io.outputChannel.valid := byteBufferValidReg

  val byteBuffer = RegInit(VecInit(Seq.fill(bufferByteSize)(0.U(8.W))))
  io.outputChannel.bits := byteBuffer

  val byteIndexCounter = RegInit(0.U(log2Ceil(bufferByteSize + 1).W))

  when (byteIndexCounter === bufferByteSize.U) {
    // We have buffered all the bytes. The output is valid.
    byteBufferValidReg := true.B
  }.elsewhen (io.inputChannel.valid) {
    // We have not buffered all bytes yet, but input is valid. We have a new byte to buffer.
    byteBuffer(byteIndexCounter) := io.inputChannel.bits
    byteIndexCounter := byteIndexCounter + 1.U
  }

  when (byteBufferValidReg && io.outputChannel.ready) {
    byteBufferValidReg := false.B
    byteIndexCounter := 0.U
  }
}
