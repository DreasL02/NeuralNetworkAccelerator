import chisel3._
import chisel3.util.{DecoupledIO, log2Ceil}
import scala_utils.Optional.optional

class OneAtATimeMatrixMultiplication(
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

    val debugInputs = optional(enableDebuggingIO, Output(Vec(numberOfRows, UInt(w.W))))
    val debugWeights = optional(enableDebuggingIO, Output(Vec(numberOfColumns, UInt(w.W))))
    val debugResults = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))))
  })

  val cyclesUntilValid: Int = numberOfColumns + numberOfRows + commonDimension - 2 // number of cycles until the systolic array is done and the result is valid


}
