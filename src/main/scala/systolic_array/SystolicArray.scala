package systolic_array

import chisel3._

class SystolicArray(w : Int = 16, dimension : Int = 4) extends Module{
  val io = IO(new Bundle {
    val a = Input(Vec(dimension, UInt(w.W)))
    val b = Input(Vec(dimension, UInt(w.W)))

    val c = Output(Vec(dimension,Vec(dimension, UInt((w+w).W))))
  })

  //https://github.com/ccelio/chisel-style-guide#vector-of-modules

  val processingElements = VecInit.fill(dimension, dimension)(Module(new ProcessingElement(w)).io)

  for (column <- 0 until dimension){
    for(row <- 0 until dimension){

      //Vertical inputs
      if(column == 0){
        //Take from buffer
        processingElements(0)(row).in_a := io.a(row)
      } else {
        //Take from previous PE
        processingElements(column)(row).in_a := processingElements(column-1)(row).out_a
      }

      //Horizontal inputs
      if (row == 0) {
        //Take from buffer
        processingElements(column)(0).in_b := io.b(column)
      } else {
        //Take from previous PE
        processingElements(column)(row).in_b := processingElements(column)(row-1).out_b
      }

      io.c(column)(row) := processingElements(column)(row).out_c
    }
  }
}
