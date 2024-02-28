package module_utils

import chisel3._

class Adders(w: Int = 8, numberOfRows: Int = 4, numberOfColumns: Int = 4) extends Module {
  val io = IO(new Bundle {
    val operandA = Input(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))
    val operandB = Input(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))

    val result = Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))
  })

  // adds the values and biases together
  for (row <- 0 until numberOfRows) {
    for (column <- 0 until numberOfColumns) {
      io.result(row)(column) := io.operandA(row)(column) + io.operandB(row)(column)
    }
  }
}