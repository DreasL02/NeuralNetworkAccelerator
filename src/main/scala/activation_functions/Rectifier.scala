package activation_functions

import chisel3._

class Rectifier(w: Int = 8, xDimension: Int = 4, yDimension: Int = 4, signed: Boolean = true) extends Module {
  val io = IO(new Bundle {
    val values = Input(Vec(xDimension, Vec(yDimension, UInt(w.W))))
    val result = Output(Vec(xDimension, Vec(yDimension, UInt(w.W))))
  })

  for (column <- 0 until xDimension) {
    for (row <- 0 until yDimension) {
      io.result(column)(row) := io.values(column)(row) //default is the same value

      if (signed) { //if the values are signed
        when(io.values(column)(row) >> (w - 1).U === 1.U) { //if signed bit (@msb) is 1, the result is negative
          io.result(column)(row) := 0.U //ReLU gives 0
        }
      }
    }
  }
}
