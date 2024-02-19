import chisel3._


class ElementWiseMultiplier(w: Int = 8, wBig: Int = 32, xDimension: Int = 4, yDimension: Int = 4, signed: Boolean = true) extends Module {
  val io = IO(new Bundle {
    val inputs = Input(Vec(xDimension, Vec(yDimension, UInt(w.W))))
    val weights = Input(Vec(xDimension, Vec(yDimension, UInt(w.W))))
    val result = Output(Vec(xDimension, Vec(yDimension, UInt(wBig.W))))
  })

  for (row <- 0 until xDimension) {
    for (column <- 0 until yDimension) {
      val multiplicationOperation = Wire(UInt((w + w).W))

      if (signed)
        multiplicationOperation := (io.inputs(row)(column).asSInt * io.weights(row)(column).asSInt).asUInt
      else
        multiplicationOperation := io.inputs(row)(column) * io.weights(row)(column)

      io.result(row)(column) := multiplicationOperation
    }
  }


}
