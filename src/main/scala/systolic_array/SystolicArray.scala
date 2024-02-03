package systolic_array

import chisel3._
import chisel3.util.log2Ceil

class SystolicArray(w: Int = 8, wStore: Int = 32, xDimension: Int = 4, yDimension: Int = 4) extends Module {
  val io = IO(new Bundle {
    val a = Input(Vec(xDimension, UInt(w.W))) //TODO see if this is the correct map
    val b = Input(Vec(yDimension, UInt(w.W)))
    val c = Output(Vec(xDimension, Vec(yDimension, UInt(w.W))))

    val signed = Input(Bool())
    val clear = Input(Bool()) // clears all registers in the PEs
  })

  // Output stationary systolic array

  // Inspired by code from:
  // https://github.com/kazutomo/Chisel-MatMul/tree/master
  // and diagrams from:
  // http://ecelabs.njit.edu/ece459/lab3.php

  // https://stackoverflow.com/questions/33621533/how-to-do-a-vector-of-modules
  val processingElements = VecInit.fill(xDimension, yDimension)(Module(new ProcessingElement(w, wStore)).io)

  for (column <- 0 until xDimension) {
    for (row <- 0 until yDimension) {
      //Vertical inputs
      if (column == 0) {
        //Take from buffer
        processingElements(0)(row).aIn := io.a(row)
      } else {
        //Take from previous PE
        processingElements(column)(row).aIn := processingElements(column - 1)(row).aOut
      }
      //Horizontal inputs
      if (row == 0) {
        //Take from buffer
        processingElements(column)(0).bIn := io.b(column)
      } else {
        //Take from previous PE
        processingElements(column)(row).bIn := processingElements(column)(row - 1).bOut
      }

      // map outputs, NB: emitted in column-major order
      io.c(column)(row) := processingElements(column)(row).cOut

      processingElements(column)(row).signed := io.signed
      processingElements(column)(row).clear := io.clear
    }
  }
}
