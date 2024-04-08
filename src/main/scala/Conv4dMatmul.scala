import chisel3._
import chisel3.util.DecoupledIO
import scala_utils.DimensionManipulation.transpose
import scala_utils.Optional.optional

class Conv4dMatmul(
                    val w: Int = 8,
                    val wResult: Int = 32,
                    val inputDimensions: (Int, Int, Int, Int) = (32, 32, 32, 32),
                    // batch size (e.g. number of images), number of input channels (e.g. RGB), height, width
                    val kernelDimensions: (Int, Int, Int, Int) = (3, 3, 3, 3),
                    // number of output channels (also called feature maps), number of input channels (e.g. RGB), height, width
                    val signed: Boolean = true, // whether the input and kernel matrices are signed
                    val strides: (Int, Int) = (1, 1), // the stride to use for the convolution
                    val pads: (Int, Int) = (0, 0), // the padding to use for the convolution
                    val enableDebuggingIO: Boolean = true
                  ) extends Module {

  assert(kernelDimensions._3 == kernelDimensions._4, "Only square kernels are supported")

  val outputDimensions = (
    inputDimensions._1, // batch size
    kernelDimensions._1, // number of output channels
    (inputDimensions._3 - kernelDimensions._3 + 2 * pads._1) / strides._1 + 1, // height
    (inputDimensions._4 - kernelDimensions._4 + 2 * pads._2) / strides._2 + 1 // width
  )

  val paddedInputDimensions = (
    inputDimensions._1,
    inputDimensions._2,
    inputDimensions._3 + 2 * pads._1,
    inputDimensions._4 + 2 * pads._2
  )

  println("inputDimensions: " + inputDimensions)
  println("paddedInputDimensions: " + paddedInputDimensions)
  println("kernelDimensions: " + kernelDimensions)
  println("outputDimensions: " + outputDimensions)


  val batchsize = paddedInputDimensions._1
  val inputChannels = paddedInputDimensions._2
  val kernelSize = kernelDimensions._3
  val inputHeight = paddedInputDimensions._3
  val inputWidth = paddedInputDimensions._4
  val outputHeight = outputDimensions._3
  val outputWidth = outputDimensions._4

  val im2colInputsDimensions = (batchsize, inputChannels * kernelSize * kernelSize, outputHeight * outputWidth)
  println("im2colInputsDimensions: " + im2colInputsDimensions)
  val im2colWeightsDimensions = (kernelDimensions._1, inputChannels * kernelSize * kernelSize)
  println("im2colWeightsDimensions: " + im2colWeightsDimensions)
  val matrixResultDimensions = (batchsize, im2colWeightsDimensions._1, im2colInputsDimensions._3)
  println("matrixResultDimensions: " + matrixResultDimensions)

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(inputDimensions._1, Vec(inputDimensions._2, Vec(inputDimensions._3, Vec(inputDimensions._4, UInt(w.W)))))))
    val kernelChannel = Flipped(new DecoupledIO(Vec(kernelDimensions._1, Vec(kernelDimensions._2, Vec(kernelDimensions._3, Vec(kernelDimensions._4, UInt(w.W)))))))
    val outputChannel = new DecoupledIO(Vec(outputDimensions._1, Vec(outputDimensions._2, Vec(outputDimensions._3, Vec(outputDimensions._4, UInt(wResult.W))))))

    val debugInputIm2Col = optional(enableDebuggingIO, Output(Vec(im2colInputsDimensions._1, Vec(im2colInputsDimensions._2, Vec(im2colInputsDimensions._3, UInt(w.W))))))
    val debugKernelIm2Col = optional(enableDebuggingIO, Output(Vec(im2colWeightsDimensions._1, Vec(im2colWeightsDimensions._2, UInt(w.W)))))
  })

  val paddedInputs = Wire(Vec(paddedInputDimensions._1, Vec(paddedInputDimensions._2, Vec(paddedInputDimensions._3, Vec(paddedInputDimensions._4, UInt(w.W))))))
  for (i <- 0 until paddedInputDimensions._1) {
    for (j <- 0 until paddedInputDimensions._2) {
      for (k <- 0 until paddedInputDimensions._3) {
        for (l <- 0 until paddedInputDimensions._4) {
          if (k < pads._1 || k >= inputDimensions._3 + pads._1 || l < pads._2 || l >= inputDimensions._4 + pads._2) {
            paddedInputs(i)(j)(k)(l) := 0.U
          } else {
            paddedInputs(i)(j)(k)(l) := io.inputChannel.bits(i)(j)(k - pads._1)(l - pads._2)
          }
        }
      }
    }
  }

  val im2colInputs = Wire(Vec(im2colInputsDimensions._1, Vec(im2colInputsDimensions._2, Vec(im2colInputsDimensions._3, UInt(w.W)))))

  // https://github.com/BVLC/caffe/files/813582/A.Simple.and.Efficient.Implementation.of.im2col.in.Convolution.Neural.Networks.pdf
  for (batch <- 0 until batchsize) {
    for (k <- 0 until inputChannels * kernelSize * kernelSize * outputHeight * outputWidth) {
      // index of outputs
      val p = k / (outputHeight * outputWidth)
      val q = k % (outputHeight * outputWidth)

      // index of inputs
      val d0 = (p / kernelSize) / kernelSize
      val i0 = q / outputWidth + (p / kernelSize) % kernelSize
      val j0 = q % outputWidth + p % kernelSize
      //println("p: " + p + ", q: " + q + ", d0: " + d0 + ", i0: " + i0 + ", j0: " + j0)

      im2colInputs(batch)(p)(q) := paddedInputs(batch)(d0)(i0)(j0)
    }
  }

  val im2colWeights = Wire(Vec(im2colWeightsDimensions._1, Vec(im2colWeightsDimensions._2, UInt(w.W))))

  for (i <- 0 until kernelDimensions._1) {
    for (j <- 0 until inputChannels * kernelSize * kernelSize) {
      im2colWeights(i)(j) := io.kernelChannel.bits(i)(j / (kernelSize * kernelSize))(j / kernelSize % kernelSize)(j % kernelSize)
    }
  }

  val result = Wire(Vec(matrixResultDimensions._1, Vec(matrixResultDimensions._2, Vec(matrixResultDimensions._3, UInt(wResult.W)))))

  val matMuls = VecInit(Seq.fill(matrixResultDimensions._1)(Module(new MatMul(w, wResult, matrixResultDimensions._2, matrixResultDimensions._3, im2colWeightsDimensions._2, signed)).io))

  for (i <- 0 until matrixResultDimensions._1) {
    matMuls(i).inputChannel.bits := im2colWeights
    matMuls(i).weightChannel.bits := im2colInputs(i)
    result(i) := matMuls(i).resultChannel.bits

    matMuls(i).inputChannel.valid := io.inputChannel.valid
    matMuls(i).weightChannel.valid := io.kernelChannel.valid
    matMuls(i).resultChannel.ready := io.outputChannel.ready
  }


  // reshape the result
  for (batch <- 0 until batchsize) {
    for (i <- 0 until outputDimensions._2) {
      for (j <- 0 until outputDimensions._3) {
        for (k <- 0 until outputDimensions._4) {
          io.outputChannel.bits(batch)(i)(j)(k) := result(batch)(i)(j * outputWidth + k)
        }
      }
    }
  }

  io.outputChannel.valid := matMuls.map(_.resultChannel.valid).reduce(_ && _)
  io.inputChannel.ready := matMuls.map(_.inputChannel.ready).reduce(_ && _) && io.outputChannel.ready && io.outputChannel.valid
  io.kernelChannel.ready := matMuls.map(_.weightChannel.ready).reduce(_ && _) && io.outputChannel.ready && io.outputChannel.valid

  if (enableDebuggingIO) {
    io.debugInputIm2Col.get := im2colInputs
    io.debugKernelIm2Col.get := im2colWeights
  }
}
