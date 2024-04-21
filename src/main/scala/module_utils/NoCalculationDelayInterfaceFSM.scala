package module_utils

import chisel3._
import chisel3.util.{Enum, is, switch}

// Mealy FSM
class NoCalculationDelayInterfaceFSM extends Module {
  val io = IO(new Bundle {
    val inputValid = Input(Bool())
    val outputReady = Input(Bool())

    val inputReady = Output(Bool())
    val outputValid = Output(Bool())
    val storeResult = Output(Bool())
  })

  private val idle :: finished :: Nil = Enum(2)
  private val state = RegInit(idle)

  io.inputReady := false.B
  io.outputValid := false.B
  io.storeResult := false.B

  switch(state) {
    is(idle) {
      io.inputReady := true.B
      when(io.inputValid) {
        // input is valid, input handshake can happen and we can start calculating and store the result in the buffer
        state := finished
        io.storeResult := true.B
      }
      // otherwise, input is not valid, wait until it is
    }
    is(finished) {
      io.outputValid := true.B
      when(io.outputReady) {
        // output handshake can happen
        state := idle
      }
      // otherwise, output handshake cannot happen yet because the output is not ready and we need to wait
    }
  }


}

