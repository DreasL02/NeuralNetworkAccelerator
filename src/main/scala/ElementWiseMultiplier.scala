import chisel3._


class ElementWiseMultiplier(w: Int = 8, xDimension: Int = 4, yDimension: Int = 4) extends Module {
  val io = IO(new Bundle {
    val inputs = Input(Vec(xDimension, Vec(yDimension, UInt(w.W))))
    val weights = Input(Vec(xDimension, Vec(yDimension, UInt(w.W))))
    val result = Output(Vec(xDimension, Vec(yDimension, UInt(w.W))))
  })

  for (row <- 0 until xDimension) {
    for (column <- 0 until yDimension) {
      io.result(row)(column) := io.inputs(row)(column) * io.weights(row)(column)
    }
  }


}
