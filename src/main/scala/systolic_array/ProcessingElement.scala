package systolic_array

import chisel3._
//http://ecelabs.njit.edu/ece459/lab3.php
class ProcessingElement(w : Int = 16) extends Module{
  val io = IO(new Bundle {
    val in_a = Input(UInt(w.W))
    val in_b = Input(UInt(w.W))

    val out_a = Output(UInt(w.W))
    val out_b = Output(UInt(w.W))
    val out_c = Output(UInt((w+w).W))
  })

  val a_reg = RegInit(0.U(w.W))
  val b_reg = RegInit(0.U(w.W))
  val c_reg = RegInit(0.U((w+w).W))

  a_reg := io.in_a
  b_reg := io.in_b
  c_reg := io.in_a * io.in_b + c_reg

  io.out_a := a_reg
  io.out_b := b_reg
  io.out_c := c_reg
}

