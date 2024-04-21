package operators

import chisel3._
import chisel3.util.DecoupledIO

class SingleChannelConvolution(
                                w: Int,
                                wResult: Int,
                                inputShape: (Int, Int), // the dimensions of the input matrix
                                kernelShape: (Int, Int), // the dimensions of the kernel matrix
                                signed: Boolean, // whether the input and kernel matrices are signed
                                strides: (Int, Int), // the stride to use for the convolution
                                pads: (Int, Int) // the padding to use for the convolution
                              ) extends Module {

  // Equation: https://builtin.com/machine-learning/fully-connected-layer
  private val outputShape = ((inputShape._1 - kernelShape._1 + 2 * pads._1) / strides._1 + 1, (inputShape._2 - kernelShape._2 + 2 * pads._2) / strides._2 + 1)

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(inputShape._1, Vec(inputShape._2, UInt(w.W)))))
    val kernelChannel = Flipped(new DecoupledIO(Vec(kernelShape._1, Vec(kernelShape._2, UInt(w.W)))))

    val outputChannel = new DecoupledIO(Vec(outputShape._1, Vec(outputShape._2, UInt(wResult.W))))
  })

  // Pad the input matrix
  private val paddedInput = Wire(Vec(inputShape._1 + 2 * pads._1, Vec(inputShape._2 + 2 * pads._2, UInt(w.W))))
  for (i <- 0 until inputShape._1 + 2 * pads._1) {
    for (j <- 0 until inputShape._2 + 2 * pads._2) {
      if (i < pads._1 || i >= inputShape._1 + pads._1 || j < pads._2 || j >= inputShape._2 + pads._2) {
        paddedInput(i)(j) := 0.U
      } else {
        paddedInput(i)(j) := io.inputChannel.bits(i - pads._1)(j - pads._2)
      }
    }
  }

  // Divide the padded input matrix into smaller matrices of the same size as the kernel matrix moving across the input matrix with the stride
  private val smallerMatrices = for (x <- 0 until outputShape._1) yield {
    for (y <- 0 until outputShape._2) yield {
      val smallerMatrix = Wire(Vec(kernelShape._1, Vec(kernelShape._2, UInt(w.W))))
      for (i <- 0 until kernelShape._1) {
        for (j <- 0 until kernelShape._2) {
          smallerMatrix(i)(j) := paddedInput(x * strides._1 + i)(y * strides._2 + j)
        }
      }
      smallerMatrix
    }
  }

  // Multiply the smaller matrices by the kernel matrix using the element wise multiplier
  private val elementWiseMultipliersResults = for (x <- 0 until outputShape._1) yield {
    for (y <- 0 until outputShape._2) yield {
      val elementWiseMultiplier = Module(new ElementWiseMultiplier(w, wResult, kernelShape._1, kernelShape._2, signed))
      elementWiseMultiplier.io.inputChannel.bits := smallerMatrices(x)(y)
      elementWiseMultiplier.io.weightChannel.bits := io.kernelChannel.bits
      elementWiseMultiplier.io.inputChannel.valid := io.inputChannel.valid
      elementWiseMultiplier.io.weightChannel.valid := io.kernelChannel.valid
      io.inputChannel.ready := elementWiseMultiplier.io.inputChannel.ready
      io.kernelChannel.ready := elementWiseMultiplier.io.weightChannel.ready
      elementWiseMultiplier.io.outputChannel
    }
  }

  // Sum of the element wise multipliers using adder trees
  private val adderTreesResults = for (x <- 0 until outputShape._1) yield {
    for (y <- 0 until outputShape._2) yield {
      val adderTree = Module(new AdderTree(wResult, kernelShape._1 * kernelShape._2))
      adderTree.io.inputChannel.bits := elementWiseMultipliersResults(x)(y).bits.flatten
      adderTree.io.inputChannel.valid := elementWiseMultipliersResults(x)(y).valid
      elementWiseMultipliersResults(x)(y).ready := adderTree.io.inputChannel.ready
      adderTree.io.outputChannel
    }
  }

  for (x <- 0 until outputShape._1) {
    for (y <- 0 until outputShape._2) {
      io.outputChannel.bits(x)(y) := adderTreesResults(x)(y).bits
      adderTreesResults(x)(y).ready := io.outputChannel.ready
    }
  }

  io.outputChannel.valid := adderTreesResults(0)(0).valid
}
