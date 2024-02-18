// Module for calculating the sum of a matrix.

import chisel3._

class Summer(w: Int = 8, xDimension: Int = 4, yDimension: Int = 4) extends Module {
  val io = IO(new Bundle {
    val inputs = Input(Vec(xDimension, Vec(yDimension, UInt(w.W))))
    val result = Output(UInt(w.W))
  })

  io.result := io.inputs.map(_.map(_.asUInt).reduce(_ + _)).reduce(_ + _)
}
