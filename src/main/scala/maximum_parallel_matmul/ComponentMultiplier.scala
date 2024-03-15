package maximum_parallel_matmul

import chisel3._
import chisel3.util.DecoupledIO

class ComponentMultiplier(
                           w: Int = 8,
                           wResult: Int = 32,
                           numberOfRows: Int = 4, // number of rows in the result matrix / number of rows in the first matrix
                           numberOfColumns: Int = 4, // number of columns in the result matrix / number of columns in the second matrix
                           commonDimension: Int = 4, // number of columns in the first matrix and number of rows in the second matrix
                           signed: Boolean = true
                         ) extends Module {
  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(numberOfRows, Vec(commonDimension, UInt(w.W)))))
    val weightChannel = Flipped(new DecoupledIO(Vec(commonDimension, Vec(numberOfColumns, UInt(w.W)))))

    val resultChannel = new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, Vec(commonDimension, UInt(wResult.W)))))
  })

  val inputs = RegNext(io.inputChannel.bits)
  val weights = RegNext(io.weightChannel.bits)

  val multiplicationResultsRegisters = RegInit(VecInit.fill(numberOfRows, numberOfColumns, commonDimension)(0.U(wResult.W)))
  for (i <- 0 until numberOfRows) {
    for (j <- 0 until numberOfColumns) {
      for (k <- 0 until commonDimension) {
        if (signed) {
          multiplicationResultsRegisters(i)(j)(k) := (inputs(i)(k).asSInt * weights(k)(j).asSInt).asUInt
        } else {
          multiplicationResultsRegisters(i)(j)(k) := inputs(i)(k) * weights(k)(j)
        }
      }
    }
  }

  io.resultChannel.bits := multiplicationResultsRegisters
  io.resultChannel.valid := RegNext(RegNext(RegNext(io.inputChannel.valid))) && RegNext(RegNext(RegNext(io.weightChannel.valid)))
  io.inputChannel.ready := io.resultChannel.ready && io.resultChannel.valid
  io.weightChannel.ready := io.resultChannel.ready && io.resultChannel.valid
}
