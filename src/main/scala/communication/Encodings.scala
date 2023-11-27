package communication

import chisel3._

object Encodings {
  object SystolicStates extends ChiselEnum {
    val idle, reading, calculating, writing, finished = Value
  }

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
    val okResponse = "b00000101".U(8.W)
  }
}