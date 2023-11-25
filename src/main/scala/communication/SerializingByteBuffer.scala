package communication

package chisel.lib.uart

import chisel3._
import chisel3.util._

class SerializingByteBuffer(bufferByteSize: Int) extends Module {
  val bufferBitSize = bufferByteSize * 8

  val io = IO(new Bundle {
    val outputChannel = new DecoupledIO(UInt(8.W))
    val inputChannel = Flipped(new DecoupledIO(Vec(bufferByteSize, UInt(8.W))))
    val debugCounterOutput = Output(UInt(8.W))
  })

  io.outputChannel.bits := DontCare

  val inputReadyReg = RegInit(true.B)
  io.inputChannel.ready := inputReadyReg

  val outputValidReg = RegInit(false.B)
  io.outputChannel.valid := outputValidReg

  val byteBuffer = RegInit(VecInit(Seq.fill(bufferByteSize)(0.U(8.W))))

  val byteIndexCounter = RegInit(0.U(log2Ceil(bufferByteSize + 1).W))

  io.debugCounterOutput := byteIndexCounter

  when (byteIndexCounter === bufferByteSize.U) {
    // We have outputted all the bytes. The output is no longer valid. We are ready to receive new bytes.
    outputValidReg := false.B
    inputReadyReg := true.B
  }.elsewhen (io.outputChannel.ready) {
    // We have not outputted all bytes yet, but output is ready. We output the next byte.
    io.outputChannel.bits := byteBuffer(byteIndexCounter)
    byteIndexCounter := byteIndexCounter + 1.U
  }

  when(io.inputChannel.valid && inputReadyReg) {
    // We are not sending bytes, and input is valid. We load all input bytes into the buffer.
    byteBuffer := io.inputChannel.bits
    byteIndexCounter := 0.U
    outputValidReg := true.B
    inputReadyReg := false.B
  }
}
