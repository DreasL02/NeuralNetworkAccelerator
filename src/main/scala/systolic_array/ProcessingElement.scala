package systolic_array

import chisel3._
import chisel3.util.log2Ceil

// Inspired by:
// https://github.com/kazutomo/Chisel-MatMul/tree/master
// and
// http://ecelabs.njit.edu/ece459/lab3.php

class ProcessingElement(w: Int = 8, wStore: Int = 32) extends Module {
  val io = IO(new Bundle {
    val aIn = Input(UInt(w.W))
    val bIn = Input(UInt(w.W))

    val aOut = Output(UInt(w.W))
    val bOut = Output(UInt(w.W))
    val cOut = Output(UInt(wStore.W))

    val signed = Input(Bool())
    val clear = Input(Bool())
  })

  val aReg = RegInit(0.U(w.W))
  val bReg = RegInit(0.U(w.W))
  val cReg = RegInit(0.U(wStore.W))

  aReg := io.aIn
  bReg := io.bIn

  when(io.signed) {
    cReg := (io.aIn.asSInt * io.bIn.asSInt).asUInt + cReg
  }.otherwise(
    cReg := io.aIn * io.bIn + cReg
  )

  io.aOut := aReg
  io.bOut := bReg
  io.cOut := cReg

  when(io.clear) {
    aReg := 0.U
    bReg := 0.U
    cReg := 0.U
  }
}

