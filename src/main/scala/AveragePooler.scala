import chisel3._

// The division in the average pooling is done through multiplication by the reciprocal
// of the divisor, which can be calculated before synthesis as the dimensions of the pooling window
// are known at that time

class AveragePooler(
                     w: Int = 8,
                     kernelShape: (Int, Int) = (2, 2),
                     pads: (Int, Int) = (0, 0),
                     strides: (Int, Int) = (2, 2),
                     xDimension: Int = 4,
                     yDimension: Int = 4,
                     signed: Boolean = true
                   ) extends Module {
  val io = IO(new Bundle {
    val inputs = Input(Vec(xDimension, Vec(yDimension, UInt(w.W))))
    val result = Output(Vec((xDimension - kernelShape._1 + 2 * pads._1) / strides._1 + 1, Vec((yDimension - kernelShape._2 + 2 * pads._2) / strides._2 + 1, UInt(w.W))))
  })

  val xOutputDimension = (xDimension - kernelShape._1 + 2 * pads._1) / strides._1 + 1
  val yOutputDimension = (yDimension - kernelShape._2 + 2 * pads._2) / strides._2 + 1

  // TODO: convert this to fixed point
  val reciprocalNumberOfKernalElements = 1.U(w.W) / (kernelShape._1 * kernelShape._2).U(w.W)

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

      val sum = Module(new Summer(w, kernelShape._1, kernelShape._2))
      sum.io.inputs := inputSlice
      if (signed) {
        io.result := (sum.io.result.asSInt * reciprocalNumberOfKernalElements.asSInt).asUInt
      }
      else {
        io.result := sum.io.result * reciprocalNumberOfKernalElements
      }
    }
  }

}