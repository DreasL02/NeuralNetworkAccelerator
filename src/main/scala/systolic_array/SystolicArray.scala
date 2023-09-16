package systolic_array

import chisel3._

class SystolicArray(w : Int = 16, horizontal : Int = 4, vertical : Int = 4) extends Module{
  val io = IO(new Bundle {
    val a = Input(Vec(horizontal, UInt(w.W)))
    val b = Input(Vec(vertical, UInt(w.W)))

    val c = Output(Vec(horizontal*vertical, UInt((w+w).W)))
  })

  //https://github.com/ccelio/chisel-style-guide#vector-of-modules

  val processingElements = VecInit.fill(horizontal, vertical)(Module(new ProcessingElement(w)).io)

  var i : Int = 0
  for (column <- 0 until horizontal){
    for(row <- 0 until vertical){

      // ---- Inputs ----
      if(column == 0){
        //Take from buffer
        processingElements(0)(row).in_a := io.a(row)
      } else {
        //Take from previous PE
        processingElements(column)(row).in_a := processingElements(column-1)(row).out_a
      }
      if (row == 0) {
        //Take from buffer
        processingElements(column)(0).in_b := io.b(column)
      } else {
        //Take from previous PE
        processingElements(column)(row).in_b := processingElements(column)(row-1).out_b
      }

      io.c(i) := processingElements(column)(row).out_c
      i = i + 1
    }
  }

}
