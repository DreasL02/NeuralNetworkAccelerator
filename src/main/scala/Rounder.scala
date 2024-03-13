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
  def this(roundType: onnx.Operators.RoundType) =
    this(roundType.wResult, roundType.wOperands, roundType.operandDimensions._1, roundType.operandDimensions._2, roundType.signed, roundType.fixedPoint)


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
          //io.output(column)(row) := sign ## (io.input(column)(row) >> fixedPoint.U)(w - 1, 0).asUInt

          // Round to nearest with round up on a tie
          io.resultChannel.bits(row)(column) := sign ## ((io.inputChannel.bits(row)(column) + (1.U << (fixedPoint.U - 1.U)).asUInt) >> fixedPoint.U)(w - 1, 0).asUInt
        }


      } else {
        if (fixedPoint == 0) {
          // No fixed point, just pass through the bottom bits
          io.resultChannel.bits(row)(column) := io.inputChannel.bits(row)(column)
        } else {
          // Round to nearest with round up on a tie
          io.resultChannel.bits(row)(column) := ((io.inputChannel.bits(row)(column) + (1.U << (fixedPoint.U - 1.U)).asUInt) >> fixedPoint.U).asUInt

          //If we want to just ceil and only use 1 shift use:
          //io.output(column)(row) := io.input(column)(row) >> io.fixedPoint
        }
      }

    }
  }
  
  io.resultChannel.ready := io.inputChannel.ready // Output is ready as soon as input is ready
  io.inputChannel.valid := io.resultChannel.ready && io.resultChannel.valid // Ready to receive new inputs when the result channel is ready and valid
}
