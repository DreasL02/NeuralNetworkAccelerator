package communication

package chisel.lib.uart

import chisel3._
import chisel3.util._

class ByteBuffer(bufferByteSize: Int = 1) extends Module {
  val bufferBitSize = bufferByteSize * 8

  val io = IO(new Bundle {
    val outputChannel = new DecoupledIO(Vec(bufferByteSize, UInt(8.W)))
    val inputChannel = Flipped(new DecoupledIO(Vec(bufferByteSize, UInt(8.W))))
  })

  io.inputChannel.ready := false.B // TODO: This is dependent on validreg?

  val buffer = RegInit(VecInit(Seq.fill(bufferByteSize)(0.U(8.W))))

  io.outputChannel.bits := buffer
  io.outputChannel.valid := false.B

  val counter = RegInit(0.U(log2Ceil(bufferByteSize + 1).W))

  val validReg = RegInit(false.B)
  io.outputChannel.valid := validReg

  when (io.inputChannel.valid) {
    io.inputChannel.ready := true.B
    buffer(counter) := io.inputChannel.bits
    counter := counter + 1.U
  }

  when (counter === bufferByteSize.U) {
    validReg := true.B
    counter := 0.U
  }

  when (io.outputChannel.ready && io.inputChannel.valid) {
    validReg := false.B
  }

}
