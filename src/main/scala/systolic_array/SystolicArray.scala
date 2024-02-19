package systolic_array

import chisel3._
import chisel3.util.log2Ceil

class SystolicArray(w: Int = 8, wStore: Int = 32, xDimension: Int = 4, yDimension: Int = 4, signed: Boolean = true) extends Module {
  val io = IO(new Bundle {
    val a = Input(Vec(yDimension, UInt(w.W))) // values shifted in from the left, equal to the number of columns in the systolic array
    val b = Input(Vec(xDimension, UInt(w.W))) // values shifted in from the top, equal to the number of rows in the systolic array
    val c = Output(Vec(xDimension, Vec(yDimension, UInt(wStore.W))))

    val clear = Input(Bool()) // clears all registers in the PEs
  })

  // Output stationary systolic array

  // Inspired by code from:
  // https://github.com/kazutomo/Chisel-MatMul/tree/master
  // and diagrams from:
  // http://ecelabs.njit.edu/ece459/lab3.php


  // https://stackoverflow.com/questions/33621533/how-to-do-a-vector-of-modules
  val processingElements = VecInit.fill(xDimension, yDimension)(Module(new ProcessingElement(w, wStore, signed)).io)

  for (row <- 0 until xDimension) {
    for (column <- 0 until yDimension) {
      //Vertical inputs
      if (row == 0) {
        //Take from buffer
        processingElements(0)(column).aIn := io.a(column)
      } else {
        //Take from previous PE
        processingElements(row)(column).aIn := processingElements(row - 1)(column).aOut
      }
      //Horizontal inputs
      if (column == 0) {
        //Take from buffer
        processingElements(row)(0).bIn := io.b(row)
      } else {
        //Take from previous PE
        processingElements(row)(column).bIn := processingElements(row)(column - 1).bOut
      }

      // map outputs
      io.c(row)(column) := processingElements(row)(column).cOut
      processingElements(row)(column).clear := io.clear
    }
  }
}
