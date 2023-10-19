import Encodings.Codes
import chisel3._
import chisel3.util.{is, switch}
import Encodings.States._
import Encodings.Codes._

class Controller extends Module {

  val io = IO(new Bundle {
    val receivedMessage = Input(Bool())
    val inputsStored = Input(Bool())
    val transmissionDone = Input(Bool())
    val readingDone = Input(Bool())
    val calculatingDone = Input(Bool())
    val writingDone = Input(Bool())
    val addressChanged = Input(Bool())

    val decodingDone = Input(Codes())
    val readMemory = Output(Bool())
    val writeMemory = Output(Bool())
  })

  val state = RegInit(receiving)
  state := state //default to current state

  // FSM state logic
  switch(state) {
    is(receiving) {
      when(io.receivedMessage) {
        state := decoding
      }
    }
    is(decoding) {
      when(io.decodingDone === nextInputs) {
        state := inputs
      }.elsewhen(io.decodingDone === nextTransmitting) {
        state := transmitting
      }.elsewhen(io.decodingDone === nextReading) {
        state := reading
      }.elsewhen(io.decodingDone === nextAddress) {
        state := address
      }
    }
    is(inputs) {
      when(io.inputsStored) {
        state := receiving
      }
    }
    is(transmitting) {
      when(io.transmissionDone) {
        state := reading
      }
    }
    is(reading) {
      when(io.readingDone) {
        state := calculating
      }
    }
    is(calculating) {
      when(io.calculatingDone) {
        state := writing
      }
    }
    is(writing) {
      when(io.writingDone) {
        state := receiving
      }
    }
    is(address) {
      when(io.addressChanged) {
        state := receiving
      }
    }
  }

  // FSM output logic
  switch(state) {
    is(receiving) {
      // Listen to uart until message arrives
    }
    is(decoding) {
      // Decode the message
    }
    is(inputs) {
      // Store input data at address in memories (single layer)
    }
    is(transmitting) {
      // Transmit data at address in memories (single layer) over uart
    }
    is(reading) {
      // Copy data at address in memories (single layer) to buffers
    }
    is(calculating) {
      // Perform computation with systolic array, addition and rectifier
    }
    is(writing) {
      // Store resulting data at address in memories (single layer)
    }
    is(address) {
      // Increment address
    }
  }
}