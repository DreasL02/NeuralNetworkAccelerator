package communication

import chisel3._
import communication.Encodings.Opcodes

class Communicator extends Module {
  val io = IO(new Bundle {
    val valid = Output(Bool())
    val opcode = Output(UInt(Opcodes.opcodeWidth.W))
  })

  io.opcode := 0.U
  when(io.valid) {
  }

}
