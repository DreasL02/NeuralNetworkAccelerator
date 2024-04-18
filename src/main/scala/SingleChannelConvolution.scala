import chisel3._
import chisel3.util.DecoupledIO
import maximum_parallel_matmul.AdderTree

class SingleChannelConvolution(
                                w: Int = 8,
                                wResult: Int = 32,
                                inputDimensions: (Int, Int) = (32, 32), // the dimensions of the input matrix
                                kernelDimensions: (Int, Int) = (3, 3), // the dimensions of the kernel matrix
                                signed: Boolean = true, // whether the input and kernel matrices are signed
                                strides: (Int, Int) = (1, 1), // the stride to use for the convolution
                                pads: (Int, Int) = (0, 0) // the padding to use for the convolution
                              ) extends Module {

  // Equation: https://builtin.com/machine-learning/fully-connected-layer
  private val outputDimensions = ((inputDimensions._1 - kernelDimensions._1 + 2 * pads._1) / strides._1 + 1, (inputDimensions._2 - kernelDimensions._2 + 2 * pads._2) / strides._2 + 1)

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(inputDimensions._1, Vec(inputDimensions._2, UInt(w.W)))))
    val kernelChannel = Flipped(new DecoupledIO(Vec(kernelDimensions._1, Vec(kernelDimensions._2, UInt(w.W)))))

    val outputChannel = new DecoupledIO(Vec(outputDimensions._1, Vec(outputDimensions._2, UInt(wResult.W))))
  })

  // Pad the input matrix
  private val paddedInput = Wire(Vec(inputDimensions._1 + 2 * pads._1, Vec(inputDimensions._2 + 2 * pads._2, UInt(w.W))))
  for (i <- 0 until inputDimensions._1 + 2 * pads._1) {
    for (j <- 0 until inputDimensions._2 + 2 * pads._2) {
      if (i < pads._1 || i >= inputDimensions._1 + pads._1 || j < pads._2 || j >= inputDimensions._2 + pads._2) {
        paddedInput(i)(j) := 0.U
      } else {
        paddedInput(i)(j) := io.inputChannel.bits(i - pads._1)(j - pads._2)
      }
    }
  }

  // Divide the padded input matrix into smaller matrices of the same size as the kernel matrix moving across the input matrix with the stride
  private val smallerMatrices = for (x <- 0 until outputDimensions._1) yield {
    for (y <- 0 until outputDimensions._2) yield {
      val smallerMatrix = Wire(Vec(kernelDimensions._1, Vec(kernelDimensions._2, UInt(w.W))))
      for (i <- 0 until kernelDimensions._1) {
        for (j <- 0 until kernelDimensions._2) {
          smallerMatrix(i)(j) := paddedInput(x * strides._1 + i)(y * strides._2 + j)
        }
      }
      smallerMatrix
    }
  }

  // Multiply the smaller matrices by the kernel matrix using the element wise multiplier
  private val elementWiseMultipliersResults = for (x <- 0 until outputDimensions._1) yield {
    for (y <- 0 until outputDimensions._2) yield {
      val elementWiseMultiplier = Module(new ElementWiseMultiplier(w, wResult, kernelDimensions._1, kernelDimensions._2, signed))
      elementWiseMultiplier.io.inputChannel.bits := smallerMatrices(x)(y)
      elementWiseMultiplier.io.weightChannel.bits := io.kernelChannel.bits
      elementWiseMultiplier.io.inputChannel.valid := io.inputChannel.valid
      elementWiseMultiplier.io.weightChannel.valid := io.kernelChannel.valid
      io.inputChannel.ready := elementWiseMultiplier.io.inputChannel.ready
      io.kernelChannel.ready := elementWiseMultiplier.io.weightChannel.ready
      elementWiseMultiplier.io.resultChannel
    }
  }

  // Sum of the element wise multipliers using adder trees
  private val adderTreesResults = for (x <- 0 until outputDimensions._1) yield {
    for (y <- 0 until outputDimensions._2) yield {
      val adderTree = Module(new AdderTree(wResult, kernelDimensions._1 * kernelDimensions._2))
      adderTree.io.inputChannel.bits := elementWiseMultipliersResults(x)(y).bits.flatten
      adderTree.io.inputChannel.valid := elementWiseMultipliersResults(x)(y).valid
      elementWiseMultipliersResults(x)(y).ready := adderTree.io.inputChannel.ready
      adderTree.io.resultChannel
    }
  }

  for (x <- 0 until outputDimensions._1) {
    for (y <- 0 until outputDimensions._2) {
      io.outputChannel.bits(x)(y) := adderTreesResults(x)(y).bits
      adderTreesResults(x)(y).ready := io.outputChannel.ready
    }
  }

  io.outputChannel.valid := adderTreesResults(0)(0).valid
}
