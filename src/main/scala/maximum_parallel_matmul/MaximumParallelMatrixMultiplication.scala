package maximum_parallel_matmul

import chisel3._
import chisel3.util.DecoupledIO


class MaximumParallelMatrixMultiplication(
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

  private val componentMultiplier = Module(new ComponentMultiplier(w, wResult, numberOfRows, numberOfColumns, commonDimension, signed))
  componentMultiplier.io.inputChannel <> io.inputChannel
  componentMultiplier.io.weightChannel <> io.weightChannel

  private val adderTrees = VecInit.fill(numberOfRows, numberOfColumns)(Module(new AdderTree(wResult, commonDimension)).io)
  for (i <- 0 until numberOfRows) {
    for (j <- 0 until numberOfColumns) {
      adderTrees(i)(j).inputChannel.bits := componentMultiplier.io.resultChannel.bits(i)(j)
      adderTrees(i)(j).inputChannel.valid := componentMultiplier.io.resultChannel.valid
      componentMultiplier.io.resultChannel.ready := adderTrees(i)(j).inputChannel.ready
      adderTrees(i)(j).resultChannel.ready := io.resultChannel.ready

      io.resultChannel.bits(i)(j) := adderTrees(i)(j).resultChannel.bits
    }
  }

  io.resultChannel.valid := adderTrees.flatMap(_.map(_.resultChannel.valid)).reduce(_ && _) // All adder trees must be valid for the result to be valid
  io.inputChannel.ready := io.resultChannel.ready && io.resultChannel.valid
  io.weightChannel.ready := io.resultChannel.ready && io.resultChannel.valid
}
