import communication.Encodings.Codes
import chisel3._
import chisel3.util.{is, switch}
import communication.Encodings.States._
import communication.Encodings.Codes._

class Controller extends Module {

  val io = IO(new Bundle {
    val receivedMessage = Input(Bool())
    val inputsStored = Input(Bool())
    val transmissionDone = Input(Bool())
    val readingDone = Input(Bool())
    val calculatingDone = Input(Bool())
    val writingDone = Input(Bool())
    val addressChanged = Input(Bool())
    val decodingCode = Input(Codes())

    val incrementAddress = Output(Bool())
    val loadBuffers = Output(Bool())
    val loadBiases = Output(Bool())
    val readMemory = Output(Bool())
    val writeMemory = Output(Bool())
  })

  val state = RegInit(receiving)
  state := state //default to keep current state

  // FSM state logic
  switch(state) {
    is(receiving) {
      when(io.receivedMessage) {
        state := decoding
      }
    }
    is(decoding) {
      when(io.decodingCode === nextInputs) {
        state := inputs
      }.elsewhen(io.decodingCode === nextTransmitting) {
        state := transmitting
      }.elsewhen(io.decodingCode === nextReading) {
        state := reading
      }.elsewhen(io.decodingCode === nextAddress) {
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

  // Default outputs
  io.incrementAddress := false.B
  io.loadBuffers := false.B
  io.loadBiases := false.B
  io.readMemory := false.B
  io.writeMemory := false.B

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
      io.writeMemory := true.B
    }
    is(transmitting) {
      // Transmit data at address in memories (single layer) over uart
      io.readMemory := true.B
    }
    is(reading) {
      // Copy data at address in memories (single layer) to buffers
      io.readMemory := true.B
      io.loadBuffers := true.B
      io.loadBiases := true.B
    }
    is(calculating) {
      // Perform computation with systolic array, addition and rectifier
      io.loadBiases := true.B // must for some reason be true
    }
    is(writing) {
      // Store resulting data at address in memories (single layer)
      io.writeMemory := true.B
    }
    is(address) {
      // Increment address
      io.incrementAddress := true.B
    }
  }
}