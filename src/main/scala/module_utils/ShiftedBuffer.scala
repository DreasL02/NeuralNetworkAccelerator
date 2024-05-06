package module_utils

import chisel3._

// This file was part of the Special course hand-in and has largely remained unchanged.

class ShiftedBuffer(w: Int, depth: Int, shift: Int) extends Module {
  val io = IO(new Bundle {
    val load = Input(Bool()) // load values?
    val data = Input(Vec(depth, UInt(w.W))) // data to be loaded
    val output = Output(UInt(w.W)) // output of the first element
  })

  private val buffer = RegInit(VecInit(Seq.fill(depth + shift)(0.U(w.W)))) // buffer of elements

  private val lastIndex = depth + shift - 1 // index of the last element

  for (i <- 0 until lastIndex) {
    buffer(i) := buffer(i + 1) // advance all elements
  }

  when(io.load) {
    for (i <- 0 until depth) {
      buffer(i + shift) := io.data(i) // load new elements in non-shifted part
    }
  }.otherwise(
    buffer(lastIndex) := 0.U // clear the last element
  )

  io.output := buffer(0) // output the first element
}