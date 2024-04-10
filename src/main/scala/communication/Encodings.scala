package communication

import chisel3._

object Encodings {
  object Codes extends ChiselEnum {
    val none, nextInputs, nextTransmitting, nextCalculating, nextAddress = Value
  }

  object Opcodes {
    val opcodeWidth = 3
    val none = "b000".U(opcodeWidth.W)
    val nextInputs = "b001".U(opcodeWidth.W)
    val nextTransmitting = "b010".U(opcodeWidth.W)
    val nextCalculating = "b011".U(opcodeWidth.W)
    val nextAddress = "b100".U(opcodeWidth.W)
  }
}