package operators

import chisel3._
import chisel3.util.DecoupledIO
import module_utils.NoCalculationDelayInterfaceFSM


// Module for doing a max pooling operation on a matrix. Arguments are: bit-width, kernel shape, pads, and strides, input and output dimensions.
class MaxPool(
               w: Int = 8,
               xDimension: Int = 32,
               yDimension: Int = 32,
               kernelShape: (Int, Int) = (2, 2),
               pads: (Int, Int) = (0, 0),
               strides: (Int, Int) = (2, 2),
               signed: Boolean = true
             ) extends Module {

  private val xOutputDimension = (xDimension - kernelShape._1 + 2 * pads._1) / strides._1 + 1
  private val yOutputDimension = (yDimension - kernelShape._2 + 2 * pads._2) / strides._2 + 1

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(xDimension, Vec(yDimension, UInt(w.W)))))
    val outputChannel = new DecoupledIO(Vec(xOutputDimension, Vec(yOutputDimension, UInt(w.W))))
  })


  val results = Wire(Vec(xOutputDimension, Vec(yOutputDimension, UInt(w.W))))
  for (i <- 0 until xOutputDimension) {
    for (j <- 0 until yOutputDimension) {
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


      if (signed) {
        results(i)(j) := inputSlice.flatten.reduce((a, b) => Mux(a.asSInt > b.asSInt, a, b))
      } else {
        results(i)(j) := inputSlice.flatten.reduce((a, b) => Mux(a > b, a, b))
      }
    }
  }

  private val interfaceFSM = Module(new NoCalculationDelayInterfaceFSM)
  interfaceFSM.io.inputValid := io.inputChannel.valid
  interfaceFSM.io.outputReady := io.outputChannel.ready

  io.outputChannel.valid := interfaceFSM.io.outputValid
  io.inputChannel.ready := interfaceFSM.io.inputReady

  val buffer = RegInit(VecInit.fill(xOutputDimension, yOutputDimension)(0.U(w.W)))
  when(interfaceFSM.io.storeResult) {
    buffer := results
  }
  io.outputChannel.bits := buffer
}
