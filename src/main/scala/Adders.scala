import chisel3._

class Adders(w: Int = 8, xDimension: Int = 4, yDimension: Int = 4) extends Module {
  val io = IO(new Bundle {
    val operandA = Input(Vec(xDimension, Vec(yDimension, UInt(w.W))))
    val operandB = Input(Vec(xDimension, Vec(yDimension, UInt(w.W))))

    val result = Output(Vec(xDimension, Vec(yDimension, UInt(w.W))))
  })

  // adds the values and biases together
  for (column <- 0 until xDimension) {
    for (row <- 0 until yDimension) {
      io.result(column)(row) := io.operandA(column)(row) + io.operandB(column)(row)
    }
  }
}