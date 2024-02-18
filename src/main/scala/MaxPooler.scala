import chisel3._


// Module for doing a max pooling operation on a matrix. Arguments are: bit-width, kernel shape, pads, and strides, input and output dimensions.
class MaxPooler(w: Int = 8, kernelShape: (Int, Int) = (2, 2), pads: (Int, Int) = (0, 0), strides: (Int, Int) = (2, 2), xDimension: Int = 4, yDimension: Int = 4) extends Module {
  val io = IO(new Bundle {
    val inputs = Input(Vec(xDimension, Vec(yDimension, UInt(w.W))))
    val result = Output(Vec((xDimension - kernelShape._1 + 2 * pads._1) / strides._1 + 1, Vec((yDimension - kernelShape._2 + 2 * pads._2) / strides._2 + 1, UInt(w.W))))
  })

  val xOutputDimension = (xDimension - kernelShape._1 + 2 * pads._1) / strides._1 + 1
  val yOutputDimension = (yDimension - kernelShape._2 + 2 * pads._2) / strides._2 + 1

  for (i <- 0 until xOutputDimension) {
    for (j <- 0 until yOutputDimension) {
      val xStart = i * strides._1
      val yStart = j * strides._2
      val xEnd = xStart + kernelShape._1
      val yEnd = yStart + kernelShape._2

      val inputSlice = VecInit.fill(kernelShape._1, kernelShape._2)(0.U(w.W))
      for (x <- xStart until xEnd) {
        for (y <- yStart until yEnd) {
          inputSlice(x - xStart)(y - yStart) := io.inputs(x)(y)
        }
      }

      val maxFinder = Module(new MaxFinder(w, kernelShape._1, kernelShape._2))

      maxFinder.io.inputs := inputSlice
      io.result(i)(j) := maxFinder.io.result
    }
  }

}
