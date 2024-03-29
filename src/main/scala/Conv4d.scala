
import chisel3._
import chisel3.util.DecoupledIO

class Conv4d(
              w: Int = 8,
              wResult: Int = 32,

              inputDimensions: (Int, Int, Int, Int) = (4, 4, 4, 4), // the dimensions of the input tensor
              // batch size (e.g. number of images), number of input channels (e.g. RGB), height, width
              kernelDimensions: (Int, Int, Int, Int) = (3, 3, 3, 3), // the dimensions of the kernel tensor
              // number of output channels (also called feature maps), number of input channels (e.g. RGB), height, width

              signed: Boolean = true, // whether the input and kernel tensors are signed
              strides: (Int, Int) = (1, 1), // the stride to use for the convolution
              pads: (Int, Int) = (0, 0) // the padding to use for the convolution
            )
  extends Module {

  assert(inputDimensions._2 == kernelDimensions._2, "The second dimension of the input and kernel tensors must be the same")


  val outputDimensions = (
    inputDimensions._1, // batch size
    kernelDimensions._1, // number of output channels
    (inputDimensions._3 - kernelDimensions._3 + 2 * pads._1) / strides._1 + 1, // height
    (inputDimensions._4 - kernelDimensions._4 + 2 * pads._2) / strides._2 + 1 // width
  )

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(inputDimensions._1, Vec(inputDimensions._2, Vec(inputDimensions._3, Vec(inputDimensions._4, UInt(w.W)))))))
    val kernelChannel = Flipped(new DecoupledIO(Vec(kernelDimensions._1, Vec(kernelDimensions._2, Vec(kernelDimensions._3, Vec(kernelDimensions._4, UInt(w.W)))))))

    val outputChannel = new DecoupledIO(Vec(outputDimensions._1, Vec(outputDimensions._2, Vec(outputDimensions._3, Vec(outputDimensions._4, UInt(wResult.W)))))
    )
  })

  val batchSize = inputDimensions._1
  val numberOfOutputChannels = kernelDimensions._1
  val numberOfConvolutions = inputDimensions._2

  println("outputDimensions: " + outputDimensions)
  println("batchSize: " + batchSize)
  println("numberOfOutputChannels: " + numberOfOutputChannels)
  println("numberOfConvolutions: " + numberOfConvolutions)

  val singleChannelConvolutions = VecInit.fill(batchSize, numberOfOutputChannels, numberOfConvolutions)(Module(new SingleChannelConvolution(w, wResult, (inputDimensions._3, inputDimensions._4), (kernelDimensions._3, kernelDimensions._4), signed, strides, pads)).io)
  val adderTree = VecInit.fill(batchSize, numberOfOutputChannels)(Module(new TensorAdderTree(wResult, numberOfConvolutions, (outputDimensions._3, outputDimensions._4))).io)


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
      io.outputChannel.bits(i)(j) := adderTree(i)(j).resultChannel.bits
      adderTree(i)(j).resultChannel.ready := io.outputChannel.ready
    }
  }

  io.inputChannel.ready := singleChannelConvolutions.map(_.map(_.map(_.inputChannel.ready).reduce(_ && _)).reduce(_ && _)).reduce(_ && _) && io.outputChannel.valid && io.outputChannel.ready
  io.kernelChannel.ready := singleChannelConvolutions.map(_.map(_.map(_.kernelChannel.ready).reduce(_ && _)).reduce(_ && _)).reduce(_ && _) && io.outputChannel.valid && io.outputChannel.ready
  io.outputChannel.valid := adderTree.map(_.map(_.resultChannel.valid).reduce(_ && _)).reduce(_ && _)
}

