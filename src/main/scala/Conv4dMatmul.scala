import chisel3._
import chisel3.util.DecoupledIO
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
                    val enableDebuggingIO: Boolean = true,
                    val print: Boolean = false
                  ) extends Module {

  def this(convType: onnx.Operators.ConvType) = this(convType.w, convType.wResult, convType.inputDimensions, convType.kernelDimensions, convType.signed, convType.strides, convType.pads, false)

  assert(inputDimensions._2 == kernelDimensions._2, "The second dimension of the input and kernel tensors must be the same")
  assert(kernelDimensions._3 == kernelDimensions._4, "Only square kernels are supported")

  private val outputDimensions = (
    inputDimensions._1, // batch size
    kernelDimensions._1, // number of output channels
    (inputDimensions._3 - kernelDimensions._3 + 2 * pads._1) / strides._1 + 1, // height
    (inputDimensions._4 - kernelDimensions._4 + 2 * pads._2) / strides._2 + 1 // width
  )

  private val paddedInputDimensions = (
    inputDimensions._1,
    inputDimensions._2,
    inputDimensions._3 + 2 * pads._1,
    inputDimensions._4 + 2 * pads._2
  )

  if (print) {
    println("inputDimensions: " + inputDimensions)
    println("paddedInputDimensions: " + paddedInputDimensions)
    println("kernelDimensions: " + kernelDimensions)
    println("outputDimensions: " + outputDimensions)
  }

  private val batchsize = paddedInputDimensions._1
  private val inputChannels = paddedInputDimensions._2
  private val kernelSize = kernelDimensions._3
  private val outputHeight = outputDimensions._3
  private val outputWidth = outputDimensions._4

  private val im2colInputsDimensions = (batchsize, inputChannels * kernelSize * kernelSize, outputHeight * outputWidth)
  private val flatKernelDimensions = (kernelDimensions._1, inputChannels * kernelSize * kernelSize)
  private val matrixResultDimensions = (batchsize, flatKernelDimensions._1, im2colInputsDimensions._3)

  if (print) {
    println("im2colInputsDimensions: " + im2colInputsDimensions)
    println("flatKernelDimensions: " + flatKernelDimensions)
    println("matrixResultDimensions: " + matrixResultDimensions)
  }

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(inputDimensions._1, Vec(inputDimensions._2, Vec(inputDimensions._3, Vec(inputDimensions._4, UInt(w.W)))))))
    val kernelChannel = Flipped(new DecoupledIO(Vec(kernelDimensions._1, Vec(kernelDimensions._2, Vec(kernelDimensions._3, Vec(kernelDimensions._4, UInt(w.W)))))))
    val outputChannel = new DecoupledIO(Vec(outputDimensions._1, Vec(outputDimensions._2, Vec(outputDimensions._3, Vec(outputDimensions._4, UInt(wResult.W))))))

    val debugInputIm2Col = optional(enableDebuggingIO, Output(Vec(im2colInputsDimensions._1, Vec(im2colInputsDimensions._2, Vec(im2colInputsDimensions._3, UInt(w.W))))))
    val debugKernel = optional(enableDebuggingIO, Output(Vec(flatKernelDimensions._1, Vec(flatKernelDimensions._2, UInt(w.W)))))
  })

  private val paddedInputs = Wire(Vec(paddedInputDimensions._1, Vec(paddedInputDimensions._2, Vec(paddedInputDimensions._3, Vec(paddedInputDimensions._4, UInt(w.W))))))
  for (batch <- 0 until paddedInputDimensions._1) {
    for (inputChannel <- 0 until paddedInputDimensions._2) {
      for (row <- 0 until paddedInputDimensions._3) {
        for (col <- 0 until paddedInputDimensions._4) {
          if (row < pads._1 || row >= inputDimensions._3 + pads._1 || col < pads._2 || col >= inputDimensions._4 + pads._2) {
            paddedInputs(batch)(inputChannel)(row)(col) := 0.U
          } else {
            paddedInputs(batch)(inputChannel)(row)(col) := io.inputChannel.bits(batch)(inputChannel)(row - pads._1)(col - pads._2)
          }
        }
      }
    }
  }

  private val im2colInputs = Wire(Vec(im2colInputsDimensions._1, Vec(im2colInputsDimensions._2, Vec(im2colInputsDimensions._3, UInt(w.W)))))

  // -------
  // im2col implementation based on non-padding algorithm presented in the following note:
  // A Simple and Efficient Implementation of im2col in Convolution Neural Networks
  // by Hao Zhang (2017).
  // https://github.com/BVLC/caffe/files/813582/A.Simple.and.Efficient.Implementation.of.im2col.in.Convolution.Neural.Networks.pdf
  // Visited on 08/04/2024
  for (batch <- 0 until batchsize) {
    for (k <- 0 until inputChannels * kernelSize * kernelSize * outputHeight * outputWidth) {
      // index of outputs
      val p = k / (outputHeight * outputWidth)
      val q = k % (outputHeight * outputWidth)

      // index of inputs
      val d0 = (p / kernelSize) / kernelSize
      val i0 = q / outputWidth + (p / kernelSize) % kernelSize
      val j0 = q % outputWidth + p % kernelSize

      im2colInputs(batch)(p)(q) := paddedInputs(batch)(d0)(i0)(j0)
    }
  }
  // -------

  // flatten the kernel
  private val flatKernel = Wire(Vec(flatKernelDimensions._1, Vec(flatKernelDimensions._2, UInt(w.W))))
  for (outputChannel <- 0 until kernelDimensions._1) {
    for (col <- 0 until inputChannels * kernelSize * kernelSize) {
      flatKernel(outputChannel)(col) := io.kernelChannel.bits(outputChannel)(col / (kernelSize * kernelSize))(col / kernelSize % kernelSize)(col % kernelSize)
    }
  }

  private val result = Wire(Vec(matrixResultDimensions._1, Vec(matrixResultDimensions._2, Vec(matrixResultDimensions._3, UInt(wResult.W)))))

  private val matMuls = VecInit(Seq.fill(matrixResultDimensions._1)(Module(new MatMul(w, wResult, matrixResultDimensions._2, matrixResultDimensions._3, flatKernelDimensions._2, signed)).io))

  for (batch <- 0 until matrixResultDimensions._1) {
    matMuls(batch).inputChannel.bits := flatKernel
    matMuls(batch).weightChannel.bits := im2colInputs(batch)
    result(batch) := matMuls(batch).resultChannel.bits

    matMuls(batch).inputChannel.valid := io.inputChannel.valid
    matMuls(batch).weightChannel.valid := io.kernelChannel.valid
    matMuls(batch).resultChannel.ready := io.outputChannel.ready
  }


  // reshape the result
  for (batch <- 0 until batchsize) {
    for (outputChannel <- 0 until outputDimensions._2) {
      for (row <- 0 until outputDimensions._3) {
        for (col <- 0 until outputDimensions._4) {
          io.outputChannel.bits(batch)(outputChannel)(row)(col) := result(batch)(outputChannel)(row * outputWidth + col)
        }
      }
    }
  }

  io.outputChannel.valid := matMuls.map(_.resultChannel.valid).reduce(_ && _)
  io.inputChannel.ready := matMuls.map(_.inputChannel.ready).reduce(_ && _) && io.outputChannel.ready && io.outputChannel.valid
  io.kernelChannel.ready := matMuls.map(_.weightChannel.ready).reduce(_ && _) && io.outputChannel.ready && io.outputChannel.valid

  if (enableDebuggingIO) {
    io.debugInputIm2Col.get := im2colInputs
    io.debugKernel.get := flatKernel
  }
}
