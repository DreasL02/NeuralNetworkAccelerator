package module_utils

import chisel3._
import chisel3.util.{Enum, is, switch}

// Mealy FSM
class CalculationDelayInterfaceFSM extends Module {
  val io = IO(new Bundle {
    val doneWithCalculation = Input(Bool())
    val inputValid = Input(Bool())
    val outputReady = Input(Bool())

    val calculateStart = Output(Bool())
    val inputReady = Output(Bool())
    val outputValid = Output(Bool())
    val storeResult = Output(Bool())
    val enableTimer = Output(Bool())
  })

  private val idle :: calculating :: finished :: Nil = Enum(3)
  private val state = RegInit(idle)

  io.calculateStart := false.B
  io.inputReady := false.B
  io.outputValid := false.B
  io.storeResult := false.B
  io.enableTimer := false.B


  switch(state) {
    is(idle) {
      io.inputReady := true.B
      when(io.inputValid) {
        // input is valid, input handshake can happen and we can start calculating
        state := calculating
        io.calculateStart := true.B
        io.enableTimer := true.B
      }
      // otherwise, input is not valid, wait until it is
    }
    is(calculating) {
      io.enableTimer := true.B
      when(io.doneWithCalculation) {
        // the number of cycles it takes to calculate the result has passed and we store the result in the buffer
        state := finished
        io.storeResult := true.B
      }
      // otherwise, we are still calculating
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

