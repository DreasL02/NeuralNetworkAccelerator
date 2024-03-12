import chisel3._
import chisel3.util.log2Ceil
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
    val inputs = Input(Vec(numberOfRows, Vec(commonDimension, UInt(w.W)))) // should only be used when load is true
    val weights = Input(Vec(commonDimension, Vec(numberOfColumns, UInt(w.W)))) // should only be used when load is true

    val result = Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))) // result of layer

    val valid = Output(Bool()) // indicates that the systolic array should be done
    val ready = Input(Bool()) // indicates that the systolic array is ready to receive new inputs
  })

  io.valid := io.ready

  val result = Wire(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W))))
  for (i <- 0 until numberOfRows) {
    for (j <- 0 until numberOfColumns) {
      if (signed) {
        result(i)(j) := io.inputs(i).zip(io.weights.map(_(j))).map { case (a, b) => a.asSInt * b.asSInt }.reduce(_ + _).asUInt
      } else {
        result(i)(j) := io.inputs(i).zip(io.weights.map(_(j))).map { case (a, b) => a * b }.reduce(_ + _)
      }
    }
  }
  io.result := result
}
