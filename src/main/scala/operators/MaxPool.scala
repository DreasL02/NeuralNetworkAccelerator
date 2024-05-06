package operators

import chisel3._
import chisel3.util.DecoupledIO
import module_utils.NoCalculationDelayInterfaceFSM


// Module for doing a max pooling operation on a matrix. Arguments are: bit-width, kernel shape, pads, and strides, input and output dimensions.
class MaxPool(
               w: Int,
               numberOfRows: Int,
               numberOfColumns: Int,
               kernelShape: (Int, Int),
               pads: (Int, Int),
               strides: (Int, Int),
               signed: Boolean
             ) extends Module {

  private val numberOfOutputRows = (numberOfRows - kernelShape._1 + 2 * pads._1) / strides._1 + 1
  private val numberOfOutputColumn = (numberOfColumns - kernelShape._2 + 2 * pads._2) / strides._2 + 1

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W)))))
    val outputChannel = new DecoupledIO(Vec(numberOfOutputRows, Vec(numberOfOutputColumn, UInt(w.W))))
  })


  val results = Wire(Vec(numberOfOutputRows, Vec(numberOfOutputColumn, UInt(w.W))))
  for (i <- 0 until numberOfOutputRows) {
    for (j <- 0 until numberOfOutputColumn) {
      val xStart = i * strides._1
      val yStart = j * strides._2
      val xEnd = xStart + kernelShape._1
      val yEnd = yStart + kernelShape._2

      val inputSlice = VecInit.fill(kernelShape._1, kernelShape._2)(0.U(w.W))
      for (x <- xStart until xEnd) {
        for (y <- yStart until yEnd) {
          inputSlice(x - xStart)(y - yStart) := io.inputChannel.bits(x)(y)
        }
      }

      val maxFinder = Module(new MaxFinderTree(w, kernelShape._1 * kernelShape._2, signed))

      maxFinder.io.inputChannel.bits := inputSlice.flatten
      maxFinder.io.inputChannel.valid := io.inputChannel.valid
      io.inputChannel.ready := maxFinder.io.inputChannel.ready
      results(i)(j) := maxFinder.io.outputChannel.bits
      maxFinder.io.outputChannel.ready := io.outputChannel.ready
      io.outputChannel.valid := maxFinder.io.outputChannel.valid
    }
  }

  io.outputChannel.bits := results
}
