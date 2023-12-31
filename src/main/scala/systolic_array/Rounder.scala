package systolic_array

import chisel3._
import chisel3.util._

class Rounder(w_target: Int = 8, w_input: Int = 16) extends Module {
  val io = IO(new Bundle {
    val fixedPoint = Input(UInt(log2Ceil(w_target).W))
    val input = Input(UInt(w_input.W))
    val output = Output(UInt(w_target.W))
  })
  when(io.fixedPoint === 0.U) {
    // No fixed point, just pass through the bottom bits
    io.output := io.input
  }.otherwise {
    // Round to nearest with round up on a tie
    io.output := ((io.input + (1.U << (io.fixedPoint - 1.U)).asUInt) >> io.fixedPoint).asUInt

    //If we want to just ceil and only use 1 shift use:
    //io.output := io.input >> io.fixedPoint
  }




}
