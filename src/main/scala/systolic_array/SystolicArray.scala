package systolic_array

import chisel3._
import chisel3.util.log2Ceil

class SystolicArray(w : Int = 8, dimension : Int = 4) extends Module {
  val io = IO(new Bundle {
    val a = Input(Vec(dimension, UInt(w.W)))
    val b = Input(Vec(dimension, UInt(w.W)))
    val c = Output(Vec(dimension,Vec(dimension, UInt(w.W))))

    val fixedPoint = Input(UInt(log2Ceil(w).W))
  })

  val processing_elements = VecInit.fill(dimension, dimension)(Module(new ProcessingElement(w)).io)

  for (column <- 0 until dimension){
    for(row <- 0 until dimension){
      //Vertical inputs
      if(column == 0){
        //Take from buffer
        processing_elements(0)(row).in_a := io.a(row)
      } else {
        //Take from previous PE
        processing_elements(column)(row).in_a := processing_elements(column-1)(row).out_a
      }
      //Horizontal inputs
      if (row == 0) {
        //Take from buffer
        processing_elements(column)(0).in_b := io.b(column)
      } else {
        //Take from previous PE
        processing_elements(column)(row).in_b := processing_elements(column)(row-1).out_b
      }

      io.c(column)(row) := processing_elements(column)(row).out_c

      processing_elements(column)(row).fixedPoint := io.fixedPoint
    }
  }
}
