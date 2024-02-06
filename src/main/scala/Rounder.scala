import chisel3._
import chisel3.util._

class Rounder(w: Int = 8, wStore: Int = 16, xDimension: Int = 4, yDimension: Int = 4) extends Module {
  val io = IO(new Bundle {
    val fixedPoint = Input(UInt(log2Ceil(w).W))
    val input = Input(Vec(xDimension, Vec(yDimension, UInt(wStore.W))))
    val output = Output(Vec(xDimension, Vec(yDimension, UInt(w.W))))
  })

  for (column <- 0 until xDimension) {
    for (row <- 0 until yDimension) {
      when(io.fixedPoint === 0.U) {
        // No fixed point, just pass through the bottom bits
        io.output(column)(row) := io.input(column)(row)
      }.otherwise {
        // Round to nearest with round up on a tie
        io.output(column)(row) := ((io.input(column)(row) + (1.U << (io.fixedPoint - 1.U)).asUInt) >> io.fixedPoint).asUInt

        //If we want to just ceil and only use 1 shift use:
        //io.output(column)(row) := io.input(column)(row) >> io.fixedPoint
      }
    }
  }
}
