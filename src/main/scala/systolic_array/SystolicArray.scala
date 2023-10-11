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

  val processingElements = VecInit.fill(dimension, dimension)(Module(new ProcessingElement(w)).io)

  for (column <- 0 until dimension){
    for(row <- 0 until dimension){
      //Vertical inputs
      if(column == 0){
        //Take from buffer
        processingElements(0)(row).aIn := io.a(row)
      } else {
        //Take from previous PE
        processingElements(column)(row).aIn := processingElements(column-1)(row).aOut
      }
      //Horizontal inputs
      if (row == 0) {
        //Take from buffer
        processingElements(column)(0).bIn := io.b(column)
      } else {
        //Take from previous PE
        processingElements(column)(row).bIn := processingElements(column)(row-1).bOut
      }

      io.c(column)(row) := processingElements(column)(row).cOut

      processingElements(column)(row).fixedPoint := io.fixedPoint
    }
  }
}
