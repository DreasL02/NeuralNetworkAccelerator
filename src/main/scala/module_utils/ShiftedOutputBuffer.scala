package module_utils

import chisel3._

class ShiftedOutputBuffer(
                           w: Int = 8,
                           dimension: Int = 4,
                           shift: Int = 4
                         ) extends Module {
  val io = IO(new Bundle {
    val data = Input(UInt(w.W))
    val output = Output(Vec(dimension, UInt(w.W)))
    val filled = Output(Bool())
  })

  private val buffer = RegInit(VecInit(Seq.fill(dimension + shift)(0.U(w.W))))

  private val lastIndex = dimension + shift - 1

  for (i <- 0 until lastIndex) {
    buffer(i) := buffer(i + 1)
  }

  
  io.output := buffer
}
