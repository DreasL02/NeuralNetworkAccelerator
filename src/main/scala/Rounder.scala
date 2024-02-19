import chisel3._
import chisel3.util._

class Rounder(
               w: Int = 8,
               wBig: Int = 16,
               xDimension: Int = 4,
               yDimension: Int = 4,
               signed: Boolean = true,
               fixedPoint: Int = 0
             ) extends Module {
  val io = IO(new Bundle {
    val input = Input(Vec(xDimension, Vec(yDimension, UInt(wBig.W))))
    val output = Output(Vec(xDimension, Vec(yDimension, UInt(w.W))))
  })


  for (column <- 0 until xDimension) {
    for (row <- 0 until yDimension) {
      if (signed) {
        val sign = io.input(column)(row)(wBig - 1)
        if (fixedPoint == 0) {
          io.output(column)(row) := sign ## io.input(column)(row)(w - 1, 0)
        } else {
          io.output(column)(row) := sign ## (io.input(column)(row) >> fixedPoint.U)(w - 1, 0).asUInt

          // Round to nearest with round up on a tie
          //io.output(column)(row) := sign ## ((io.input(column)(row) + (1.U << (io.fixedPoint - 1.U)).asUInt) >> io.fixedPoint)(w - 2, 0).asUInt
        }


      } else {
        if (fixedPoint == 0) {
          // No fixed point, just pass through the bottom bits
          io.output(column)(row) := io.input(column)(row)
        } else {
          // Round to nearest with round up on a tie
          io.output(column)(row) := ((io.input(column)(row) + (1.U << (fixedPoint.U - 1.U)).asUInt) >> fixedPoint.U).asUInt

          //If we want to just ceil and only use 1 shift use:
          //io.output(column)(row) := io.input(column)(row) >> io.fixedPoint
        }
      }

    }
  }
}
