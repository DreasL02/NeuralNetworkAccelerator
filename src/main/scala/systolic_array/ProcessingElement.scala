package systolic_array

import chisel3._
import chisel3.util.{Fill, log2Ceil}
import module_utils.SmallModules.mult

// Inspired by:
// https://github.com/kazutomo/Chisel-MatMul/tree/master
// and
// http://ecelabs.njit.edu/ece459/lab3.php

class ProcessingElement(
                         w: Int = 8, // width of the inputs
                         wResult: Int = 32, // width of the result / register
                         signed: Boolean = true // to determine if signed or unsigned multiplication should be used
                       ) extends Module {

  assert(wResult >= w * 2, "wResult must be at least twice the width of w")

  val io = IO(new Bundle {
    val aIn = Input(UInt(w.W))
    val bIn = Input(UInt(w.W))

    val aOut = Output(UInt(w.W))
    val bOut = Output(UInt(w.W))
    val cOut = Output(UInt(wResult.W))

    val clear = Input(Bool())
  })

  val aReg = RegInit(0.U(w.W))
  val bReg = RegInit(0.U(w.W))
  val cReg = RegInit(0.U(wResult.W))
  
  aReg := io.aIn // stagger the inputs
  bReg := io.bIn // stagger the inputs
  cReg := mult(io.aIn, io.bIn, w, wResult, signed) + cReg // MAC operation

  io.aOut := aReg
  io.bOut := bReg
  io.cOut := cReg

  when(io.clear) {
    aReg := 0.U
    bReg := 0.U
    cReg := 0.U
  }
}

