package operators

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

    val outputChannel = new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W))))
  })

  private val componentMultiplier = Module(new ComponentMultiplier(w, wResult, numberOfRows, numberOfColumns, commonDimension, signed))
  componentMultiplier.io.inputChannel <> io.inputChannel
  componentMultiplier.io.weightChannel <> io.weightChannel

  private val adderTrees = VecInit.fill(numberOfRows, numberOfColumns)(Module(new AdderTree(wResult, commonDimension)).io)
  for (i <- 0 until numberOfRows) {
    for (j <- 0 until numberOfColumns) {
      adderTrees(i)(j).inputChannel.bits := componentMultiplier.io.outputChannel.bits(i)(j)
      adderTrees(i)(j).inputChannel.valid := componentMultiplier.io.outputChannel.valid
      componentMultiplier.io.outputChannel.ready := adderTrees(i)(j).inputChannel.ready
      adderTrees(i)(j).outputChannel.ready := io.outputChannel.ready

      io.outputChannel.bits(i)(j) := adderTrees(i)(j).outputChannel.bits
    }
  }

  // when one of the adder trees is done, all of them are done
  io.outputChannel.valid := adderTrees(0)(0).outputChannel.valid
}
