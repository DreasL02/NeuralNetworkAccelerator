import chisel3._

class Accumulator(w : Int = 16, dimension : Int = 4) extends Module{
  val io = IO(new Bundle {
    val values = Input(Vec(dimension, Vec(dimension, UInt((w + w).W))))
    val biases = Input(Vec(dimension, Vec(dimension, UInt((w + w).W))))

    val result = Output(Vec(dimension, Vec(dimension, UInt((w + w).W))))
  })

  for (column <- 0 until dimension) {
    for (row <- 0 until dimension) {
      io.result(column)(row) := io.values(column)(row) + io.biases(column)(row)
    }
  }
}