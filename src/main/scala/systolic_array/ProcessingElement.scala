package systolic_array

import chisel3._
import chisel3.util.log2Ceil
//http://ecelabs.njit.edu/ece459/lab3.php
class ProcessingElement(w : Int = 8) extends Module{
  val io = IO(new Bundle {
    val in_a = Input(UInt(w.W))
    val in_b = Input(UInt(w.W))

    val out_a = Output(UInt(w.W))
    val out_b = Output(UInt(w.W))
    val out_c = Output(UInt(w.W))

    val fixedPoint = Input(UInt(log2Ceil(w).W))
  })
  val rounder = Module(new Rounder(w_target = w, w_input = w+w+1))

  val aReg = RegInit(0.U(w.W))
  val bReg = RegInit(0.U(w.W))
  val cReg = RegInit(0.U((w+w).W))

  aReg := io.in_a
  bReg := io.in_b
  cReg := io.in_a * io.in_b + cReg

  io.out_a := aReg
  io.out_b := bReg

  rounder.io.input := cReg
  rounder.io.fixedPoint := io.fixedPoint

  io.out_c := rounder.io.output
}

