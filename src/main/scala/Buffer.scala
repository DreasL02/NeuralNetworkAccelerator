 import chisel3._

class Buffer(w : Int = 8, dimension : Int = 4, shift : Int) extends Module{
  val io = IO(new Bundle {
    val load = Input(Bool())
    val data = Input(Vec(dimension, UInt(w.W)))
    val output = Output(UInt(w.W))
  })

  val buffer = RegInit(VecInit(Seq.fill(dimension+2)(0.U(w.W))))

  for (i <- 0 until dimension + 1) {
    buffer(i + 1) := buffer(i)
  }

  when(io.load){
    for (i <- shift until dimension+shift) {
      buffer(i) := io.data(i)
    }
  }.otherwise(
    buffer(dimension+1) := 0.U
  )

  io.output := buffer(0)
}