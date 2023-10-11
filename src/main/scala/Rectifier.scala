import chisel3._
class Rectifier(w : Int = 8, dimension : Int = 4) extends Module{
  val io = IO(new Bundle {
    val values = Input(Vec(dimension, Vec(dimension, UInt(w.W))))
    val signed = Input(Bool())
    val result = Output(Vec(dimension, Vec(dimension, UInt(w.W))))
  })

  for (column <- 0 until dimension) {
    for (row <- 0 until dimension) {
      io.result(column)(row) := io.values(column)(row)

      when(io.signed){ //if the values are signed
        when(io.values(column)(row) >> (w-1).U === 1.U) { //if signed bit is 1, the result is negative
          io.result(column)(row) := 0.U
        }
      }
    }
  }
}
