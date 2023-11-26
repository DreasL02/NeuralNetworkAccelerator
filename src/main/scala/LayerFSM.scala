import communication.Encodings.Codes
import chisel3._
import chisel3.util.{is, switch}
import communication.Encodings.SystolicStates._

class LayerFSM extends Module {

  val io = IO(new Bundle {
    // To communication
    val start = Input(Bool())
    val finished = Output(Bool())

    // To layer calculator
    val calculatingDone = Input(Bool())
    val incrementAddress = Output(Bool())
    val loadBuffers = Output(Bool())
    val readMemory = Output(Bool())
    val writeMemory = Output(Bool())
  })

  val state = RegInit(idle)
  state := state //default to keep current state

  // FSM state logic
  switch(state) {
    is(idle) {
      when(io.start) {
        state := reading
      }
    }
    is(reading) {
      // should take 1 cycle with register memory
      state := calculating
    }
    is(calculating) {
      when(io.calculatingDone) {
        state := writing
      }
    }
    is(writing) {
      // should take 1 cycle with register memory
      state := finished

    }
    is(finished) {
      state := idle
    }
  }

  // Default outputs
  io.incrementAddress := false.B
  io.loadBuffers := false.B
  io.readMemory := false.B
  io.writeMemory := false.B
  io.finished := false.B

  // FSM output logic
  switch(state) {
    is(idle) {

    }
    is(reading) {
      // Copy data at address in memories (single layer) to buffers
      io.readMemory := true.B
      io.loadBuffers := true.B
      io.incrementAddress := true.B
      // TODO: See if increment address is a good idea, as it would lead to storing in the next layers
      // TODO: input but could cause issues with timing?
    }
    is(calculating) {
      // Perform computation with systolic array, addition and rectifier
    }
    is(writing) {
      // Store resulting data at address in memories (single layer)
      io.writeMemory := true.B
    }
    is(finished) {
      io.finished := true.B
    }
  }
}