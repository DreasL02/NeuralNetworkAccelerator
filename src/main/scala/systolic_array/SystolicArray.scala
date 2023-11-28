package systolic_array

import chisel3._
import chisel3.util.log2Ceil

class SystolicArray(w: Int = 8, dimension: Int = 4) extends Module {
  val io = IO(new Bundle {
    val a = Input(Vec(dimension, UInt(w.W)))
    val b = Input(Vec(dimension, UInt(w.W)))
    val c = Output(Vec(dimension, Vec(dimension, UInt(w.W))))

    val fixedPoint = Input(UInt(log2Ceil(w).W))
    val clear = Input(Bool()) // clears all registers in the PEs
  })

  // Inspired by:
  // https://github.com/kazutomo/Chisel-MatMul/tree/master
  // and
  // http://ecelabs.njit.edu/ece459/lab3.php

  val processingElements = VecInit.fill(dimension, dimension)(Module(new ProcessingElement(w)))

  for (column <- 0 until dimension) {
    for (row <- 0 until dimension) {
      //Vertical inputs
      if (column == 0) {
        //Take from buffer
        processingElements(0)(row).io.aIn := io.a(row)
      } else {
        //Take from previous PE
        processingElements(column)(row).io.aIn := processingElements(column - 1)(row).io.aOut
      }
      //Horizontal inputs
      if (row == 0) {
        //Take from buffer
        processingElements(column)(0).io.bIn := io.b(column)
      } else {
        //Take from previous PE
        processingElements(column)(row).io.bIn := processingElements(column)(row - 1).io.bOut
      }

      // map outputs
      io.c(column)(row) := processingElements(column)(row).io.cOut

      processingElements(column)(row).io.fixedPoint := io.fixedPoint
      processingElements(column)(row).io.clear := io.clear
    }
  }
}
