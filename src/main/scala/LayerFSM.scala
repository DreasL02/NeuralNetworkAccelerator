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
    val loadBuffers = Output(Bool())
    val readMemory = Output(Bool())
    val writeMemory = Output(Bool())

    // To address
    val incrementAddress = Output(Bool())
  })

  val state = RegInit(idle)
  state := state //default to keep current state

  // Default outputs
  io.incrementAddress := false.B
  io.loadBuffers := false.B
  io.readMemory := false.B
  io.writeMemory := false.B
  io.finished := false.B

  switch(state) {
    is(idle) {
      when(io.start) {
        state := reading
      }
    }
    is(reading) {
      // should take 1 cycle with register memory
      state := calculating
      io.readMemory := true.B
      io.loadBuffers := true.B
      io.incrementAddress := true.B
    }
    is(calculating) {
      when(io.calculatingDone) { //should take NxN - 1 cycles to be asserted
        state := writing
      }
    }
    is(writing) {
      // should take 1 cycle with register memory
      state := finished
      io.writeMemory := true.B
    }
    is(finished) {
      state := idle
      io.finished := true.B
    }
  }
}