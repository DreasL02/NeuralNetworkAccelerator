import chisel3._
import chisel3.util._

class Rounder(
               w: Int = 8,
               wBig: Int = 16,
               numberOfRows: Int = 4,
               numberOfColumns: Int = 4,
               signed: Boolean = true,
               fixedPoint: Int = 0
             ) extends Module {

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(wBig.W)))))

    val resultChannel = new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))
  })


  for (row <- 0 until numberOfRows) {
    for (column <- 0 until numberOfColumns) {
      if (signed) {
        val sign = io.inputChannel.bits(row)(column)(wBig - 1)
        if (fixedPoint == 0) {
          io.resultChannel.bits(row)(column) := sign ## io.inputChannel.bits(row)(column)(w - 1, 0)
        } else {
          io.resultChannel.bits(row)(column) := sign ## ((io.inputChannel.bits(row)(column) + (1.U << (fixedPoint.U - 1.U)).asUInt) >> fixedPoint.U)(w - 1, 0).asUInt
        }


      } else {
        if (fixedPoint == 0) {
          io.resultChannel.bits(row)(column) := io.inputChannel.bits(row)(column)
        } else {
          io.resultChannel.bits(row)(column) := ((io.inputChannel.bits(row)(column) + (1.U << (fixedPoint.U - 1.U)).asUInt) >> fixedPoint.U).asUInt
        }
      }

    }
  }

  io.resultChannel.valid := io.inputChannel.valid // Output is valid as soon as input is valid
  io.inputChannel.ready := io.resultChannel.ready && io.resultChannel.valid // Ready to receive new inputs when the result channel is ready and valid
}
