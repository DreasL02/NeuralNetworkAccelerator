package systolic_array

import chisel3._
import chisel3.util.log2Ceil

class SystolicArray(
                     w: Int = 8, // width of the inputs
                     wResult: Int = 32, // width of the result
                     numberOfRows: Int = 4, // number of rows in the result matrix (number of PEs in the vertical direction)
                     numberOfColumns: Int = 4, // number of columns in the result matrix (number of PEs in the horizontal direction)
                     signed: Boolean = true // to determine if signed or unsigned multiplication should be used
                   ) extends Module {
  val io = IO(new Bundle {
    val a = Input(Vec(numberOfRows, UInt(w.W))) // values shifted in from the left, equal to the number of rows in the systolic array
    val b = Input(Vec(numberOfColumns, UInt(w.W))) // values shifted in from the top, equal to the number of columns in the systolic array
    val c = Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))) // result of matrix multiplication

    val clear = Input(Bool()) // clears all registers in the PEs
  })

  // Output stationary systolic array

  // Inspired by code from:
  // https://github.com/kazutomo/Chisel-MatMul/tree/master
  // and diagrams from:
  // http://ecelabs.njit.edu/ece459/lab3.php


  // https://stackoverflow.com/questions/33621533/how-to-do-a-vector-of-modules
  val processingElements = VecInit.fill(numberOfRows, numberOfColumns)(Module(new ProcessingElement(w, wResult, signed)).io)

  for (row <- 0 until numberOfRows) {
    for (column <- 0 until numberOfColumns) {
      //Vertical inputs
      if (column == 0) {
        //Take from buffer
        processingElements(row)(0).aIn := io.a(row)
      } else {
        //Take from previous PE
        processingElements(row)(column).aIn := processingElements(row)(column - 1).aOut
      }
      //Horizontal inputs
      if (row == 0) {
        //Take from buffer
        processingElements(0)(column).bIn := io.b(column)
      } else {
        //Take from previous PE
        processingElements(row)(column).bIn := processingElements(row - 1)(column).bOut
      }

      // map outputs
      io.c(row)(column) := processingElements(row)(column).cOut
      processingElements(row)(column).clear := io.clear
    }
  }
}
