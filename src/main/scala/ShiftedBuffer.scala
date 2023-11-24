import chisel3._

class ShiftedBuffer(w: Int = 8, dimension: Int = 4, shift: Int) extends Module {
  val io = IO(new Bundle {
    val load = Input(Bool())
    val data = Input(Vec(dimension, UInt(w.W)))
    val output = Output(UInt(w.W))
  })

  val buffer = RegInit(VecInit(Seq.fill(dimension + shift)(0.U(w.W))))

  val lastIndex = dimension + shift - 1

  for (i <- 0 until lastIndex) {
    buffer(i) := buffer(i + 1)
  }

  when(io.load) {
    for (i <- 0 until dimension) {
      buffer(i + shift) := io.data(i)
    }
  }.otherwise(
    buffer(lastIndex) := 0.U
  )

  io.output := buffer(0)
}