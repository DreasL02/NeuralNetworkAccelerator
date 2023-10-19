import chisel3._

object Encodings {
  object States extends ChiselEnum {
    val receiving, decoding, inputs, transmitting, reading, calculating, writing, address = Value
  }

  object Codes extends ChiselEnum {
    val nextInputs, nextTransmitting, nextReading, nextAddress = Value
  }
}