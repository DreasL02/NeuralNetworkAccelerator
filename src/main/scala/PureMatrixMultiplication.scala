import chisel3._
import chisel3.util.{DecoupledIO, log2Ceil}
import scala_utils.Optional._


class PureMatrixMultiplication(
                                w: Int = 8,
                                wResult: Int = 32,
                                numberOfRows: Int = 4, // number of rows in the result matrix / number of rows in the first matrix
                                numberOfColumns: Int = 4, // number of columns in the result matrix / number of columns in the second matrix
                                commonDimension: Int = 4, // number of columns in the first matrix and number of rows in the second matrix
                                signed: Boolean = true,
                                enableDebuggingIO: Boolean = true
                              ) extends Module {
  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(numberOfRows, Vec(commonDimension, UInt(w.W)))))
    val weightChannel = Flipped(new DecoupledIO(Vec(commonDimension, Vec(numberOfColumns, UInt(w.W)))))

    val resultChannel = new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W))))
  })

  val inputs = RegNext(io.inputChannel.bits)
  val weights = RegNext(io.weightChannel.bits)

  val result = Wire(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W))))
  for (i <- 0 until numberOfRows) {
    for (j <- 0 until numberOfColumns) {
      if (signed) {
        result(i)(j) := inputs(i).zip(weights.map(_(j))).map { case (a, b) => a.asSInt * b.asSInt }.reduce(_ + _).asUInt
      } else {
        result(i)(j) := inputs(i).zip(weights.map(_(j))).map { case (a, b) => a * b }.reduce(_ + _)
      }
    }
  }
  io.resultChannel.bits := RegNext(result)

  io.resultChannel.valid := RegNext(RegNext(io.inputChannel.valid)) && RegNext(RegNext(io.weightChannel.valid))
  io.inputChannel.ready := io.resultChannel.ready && io.resultChannel.valid
  io.weightChannel.ready := io.resultChannel.ready && io.resultChannel.valid
}
