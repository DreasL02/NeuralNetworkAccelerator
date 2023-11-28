import chisel3._

class ShiftedBuffer(w: Int = 8, dimension: Int = 4, shift: Int) extends Module {
  val io = IO(new Bundle {
    val load = Input(Bool())                    // load values?
    val data = Input(Vec(dimension, UInt(w.W))) // data to be loaded
    val output = Output(UInt(w.W))              // output of the first element
  })

  val buffer = RegInit(VecInit(Seq.fill(dimension + shift)(0.U(w.W)))) // buffer of elements

  val lastIndex = dimension + shift - 1 // index of the last element

  for (i <- 0 until lastIndex) {
    buffer(i) := buffer(i + 1) // advance all elements
  }

  when(io.load) {
    for (i <- 0 until dimension) {
      buffer(i + shift) := io.data(i) // load new elements in non-shifted part
    }
  }.otherwise(
    buffer(lastIndex) := 0.U // clear the last element
  )

  io.output := buffer(0) // output the first element
}