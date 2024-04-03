package communication

import chisel3._
import chisel3.util.{is, switch}
import communication.Encodings.Codes
import communication.Encodings.Opcodes

// TODO: Remove. Unused.
class Decoder extends Module {
  val io = IO(new Bundle {
    val opcode = Input(UInt(Opcodes.opcodeWidth.W))
    val decodingCode = Output(Codes())
  })

  io.decodingCode := Codes.none
  switch(io.opcode) {
    is(Opcodes.nextInputs) {
      io.decodingCode := Codes.nextInputs
    }
    is(Opcodes.nextTransmitting) {
      io.decodingCode := Codes.nextTransmitting
    }
    is(Opcodes.nextCalculating) {
      io.decodingCode := Codes.nextCalculating
    }
    is(Opcodes.nextAddress) {
      io.decodingCode := Codes.nextAddress
    }
  }
}
