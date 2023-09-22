import chisel3._
class Rectifier(w : Int = 16, dimension : Int = 4) extends Module{
  val io = IO(new Bundle {
    val values = Input(Vec(dimension, Vec(dimension, UInt((w + w).W))))

    val result = Output(Vec(dimension, Vec(dimension, UInt((w + w).W))))
  })

  for (column <- 0 until dimension) {
    for (row <- 0 until dimension) {
      io.result(column)(row) := io.values(column)(row)
      when(io.values(column)(row) < 0.U){
        io.result(column)(row) := 0.U
      }
    }
  }
}
