package operators


import chisel3._
import chisel3.util.DecoupledIO

class ConvDirect(
                  w: Int,
                  wResult: Int,
                  inputShape: (Int, Int, Int, Int), // the dimensions of the input tensor
                  // batch size (e.g. number of images), number of input channels (e.g. RGB), height, width
                  kernelShape: (Int, Int, Int, Int), // the dimensions of the kernel tensor
                  // number of output channels (also called feature maps), number of input channels (e.g. RGB), height, width

                  signed: Boolean, // whether the input and kernel tensors are signed
                  strides: (Int, Int), // the stride to use for the convolution
                  pads: (Int, Int), // the padding to use for the convolution
                  print: Boolean = false
                )
  extends Module {

  assert(inputShape._2 == kernelShape._2, "The second dimension of the input and kernel tensors must be the same")

  val outputDimensions = (
    inputShape._1, // batch size
    kernelShape._1, // number of output channels
    (inputShape._3 - kernelShape._3 + 2 * pads._1) / strides._1 + 1, // height
    (inputShape._4 - kernelShape._4 + 2 * pads._2) / strides._2 + 1 // width
  )

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(inputShape._1, Vec(inputShape._2, Vec(inputShape._3, Vec(inputShape._4, UInt(w.W)))))))
    val kernelChannel = Flipped(new DecoupledIO(Vec(kernelShape._1, Vec(kernelShape._2, Vec(kernelShape._3, Vec(kernelShape._4, UInt(w.W)))))))

    val outputChannel = new DecoupledIO(Vec(outputDimensions._1, Vec(outputDimensions._2, Vec(outputDimensions._3, Vec(outputDimensions._4, UInt(wResult.W)))))
    )
  })

  private val batchSize = inputShape._1
  private val numberOfOutputChannels = kernelShape._1
  private val numberOfConvolutions = inputShape._2

  if (print) {
    println("outputDimensions: " + outputDimensions)
    println("batchSize: " + batchSize)
    println("numberOfOutputChannels: " + numberOfOutputChannels)
    println("numberOfConvolutions: " + numberOfConvolutions)
  }

  private val singleChannelConvolutions = VecInit.fill(batchSize, numberOfOutputChannels, numberOfConvolutions)(Module(new SingleChannelConvolution(w, wResult, (inputShape._3, inputShape._4), (kernelShape._3, kernelShape._4), signed, strides, pads)).io)
  private val adderTree = VecInit.fill(batchSize, numberOfOutputChannels)(Module(new TensorAdderTree(wResult, numberOfConvolutions, (outputDimensions._3, outputDimensions._4))).io)

  for (i <- 0 until batchSize) {
    for (j <- 0 until numberOfOutputChannels) {
      for (k <- 0 until numberOfConvolutions) {
        singleChannelConvolutions(i)(j)(k).inputChannel.valid := io.inputChannel.valid
        singleChannelConvolutions(i)(j)(k).inputChannel.bits := io.inputChannel.bits(i)(k)
        singleChannelConvolutions(i)(j)(k).kernelChannel.valid := io.kernelChannel.valid
        singleChannelConvolutions(i)(j)(k).kernelChannel.bits := io.kernelChannel.bits(j)(k)

        adderTree(i)(j).inputChannel.bits(k) := singleChannelConvolutions(i)(j)(k).outputChannel.bits
        singleChannelConvolutions(i)(j)(k).outputChannel.ready := adderTree(i)(j).inputChannel.ready
      }
      adderTree(i)(j).inputChannel.valid := singleChannelConvolutions(i)(j).map(_.outputChannel.valid).reduce(_ && _)
      io.outputChannel.bits(i)(j) := adderTree(i)(j).outputChannel.bits
      adderTree(i)(j).outputChannel.ready := io.outputChannel.ready
    }
  }

  io.inputChannel.ready := singleChannelConvolutions(0)(0)(0).inputChannel.ready
  io.kernelChannel.ready := singleChannelConvolutions(0)(0)(0).kernelChannel.ready
  io.outputChannel.valid := singleChannelConvolutions(0)(0)(0).outputChannel.valid
}

