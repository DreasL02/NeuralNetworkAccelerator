package activation_functions

import chisel3._

class ReLU(w: Int = 8, numberOfRows: Int = 4, numberOfColumns: Int = 4, signed: Boolean = true) extends Module {
  val io = IO(new Bundle {
    val input = Input(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))
    val result = Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))
  })

  for (row <- 0 until numberOfRows) {
    for (column <- 0 until numberOfColumns) {
      io.result(row)(column) := io.input(row)(column) //default is the same value

      if (signed) { //if the values are signed
        when(io.input(row)(column) >> (w - 1).U === 1.U) { //if signed bit (@msb) is 1, the result is negative
          io.result(row)(column) := 0.U //ReLU gives 0
        }
      }
    }
  }
}
