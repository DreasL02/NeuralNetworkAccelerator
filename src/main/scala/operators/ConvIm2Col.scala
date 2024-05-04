package operators

import chisel3._
import chisel3.util.DecoupledIO
import operators.systolic_array.BufferedSystolicArray
import scala_utils.Optional.optional

class ConvIm2Col(
                  val w: Int,
                  val wResult: Int,
                  val inputShape: (Int, Int, Int, Int),
                  // batch size (e.g. number of images), number of input channels (e.g. RGB), height, width
                  val kernelShape: (Int, Int, Int, Int),
                  // number of output channels (also called feature maps), number of input channels (e.g. RGB), height, width
                  val signed: Boolean, // whether the input and kernel matrices are signed
                  val strides: (Int, Int), // the stride to use for the convolution
                  val pads: (Int, Int), // the padding to use for the convolution
                  val enableDebuggingIO: Boolean = false,
                  val print: Boolean = false
                ) extends Module {

  assert(inputShape._2 == kernelShape._2, "The second dimension of the input and kernel tensors must be the same")
  assert(kernelShape._3 == kernelShape._4, "Only square kernels are supported")
  assert(strides._1 == 1, "Only stride of 1 is supported")
  assert(strides._2 == 1, "Only stride of 1 is supported")

  private val outputDimensions = (
    inputShape._1, // batch size
    kernelShape._1, // number of output channels
    (inputShape._3 - kernelShape._3 + 2 * pads._1) / strides._1 + 1, // height
    (inputShape._4 - kernelShape._4 + 2 * pads._2) / strides._2 + 1 // width
  )

  private val paddedInputDimensions = (
    inputShape._1,
    inputShape._2,
    inputShape._3 + 2 * pads._1,
    inputShape._4 + 2 * pads._2
  )

  if (print) {
    println("inputDimensions: " + inputShape)
    println("paddedInputDimensions: " + paddedInputDimensions)
    println("kernelDimensions: " + kernelShape)
    println("outputDimensions: " + outputDimensions)
  }

  private val batchsize = paddedInputDimensions._1
  private val inputChannels = paddedInputDimensions._2
  private val kernelSize = kernelShape._3
  private val outputHeight = outputDimensions._3
  private val outputWidth = outputDimensions._4

  private val im2colInputsDimensions = (batchsize, inputChannels * kernelSize * kernelSize, outputHeight * outputWidth)
  private val flatKernelDimensions = (kernelShape._1, inputChannels * kernelSize * kernelSize)
  private val matrixResultDimensions = (batchsize, flatKernelDimensions._1, im2colInputsDimensions._3)

  if (print) {
    println("im2colInputsDimensions: " + im2colInputsDimensions)
    println("flatKernelDimensions: " + flatKernelDimensions)
    println("matrixResultDimensions: " + matrixResultDimensions)
  }

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(inputShape._1, Vec(inputShape._2, Vec(inputShape._3, Vec(inputShape._4, UInt(w.W)))))))
    val kernelChannel = Flipped(new DecoupledIO(Vec(kernelShape._1, Vec(kernelShape._2, Vec(kernelShape._3, Vec(kernelShape._4, UInt(w.W)))))))
    val outputChannel = new DecoupledIO(Vec(outputDimensions._1, Vec(outputDimensions._2, Vec(outputDimensions._3, Vec(outputDimensions._4, UInt(wResult.W))))))

    val debugInputIm2Col = optional(enableDebuggingIO, Output(Vec(im2colInputsDimensions._1, Vec(im2colInputsDimensions._2, Vec(im2colInputsDimensions._3, UInt(w.W))))))
    val debugKernel = optional(enableDebuggingIO, Output(Vec(flatKernelDimensions._1, Vec(flatKernelDimensions._2, UInt(w.W)))))
  })

  private val paddedInputs = Wire(Vec(paddedInputDimensions._1, Vec(paddedInputDimensions._2, Vec(paddedInputDimensions._3, Vec(paddedInputDimensions._4, UInt(w.W))))))
  for (batch <- 0 until paddedInputDimensions._1) {
    for (inputChannel <- 0 until paddedInputDimensions._2) {
      for (row <- 0 until paddedInputDimensions._3) {
        for (col <- 0 until paddedInputDimensions._4) {
          if (row < pads._1 || row >= inputShape._3 + pads._1 || col < pads._2 || col >= inputShape._4 + pads._2) {
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
  for (outputChannel <- 0 until kernelShape._1) {
    for (col <- 0 until inputChannels * kernelSize * kernelSize) {
      flatKernel(outputChannel)(col) := io.kernelChannel.bits(outputChannel)(col / (kernelSize * kernelSize))(col / kernelSize % kernelSize)(col % kernelSize)
    }
  }

  private val result = Wire(Vec(matrixResultDimensions._1, Vec(matrixResultDimensions._2, Vec(matrixResultDimensions._3, UInt(wResult.W)))))

  // create the matrix multipliers, using the systolic array as direct matmul would not gain any performance when compared to the direct conv
  private val matMuls = VecInit(Seq.fill(matrixResultDimensions._1)(Module(new BufferedSystolicArray(w, wResult, matrixResultDimensions._2, matrixResultDimensions._3, flatKernelDimensions._2, signed)).io))

  for (batch <- 0 until matrixResultDimensions._1) {
    matMuls(batch).inputChannel.bits := flatKernel
    matMuls(batch).weightChannel.bits := im2colInputs(batch)
    result(batch) := matMuls(batch).outputChannel.bits

    matMuls(batch).inputChannel.valid := io.inputChannel.valid
    matMuls(batch).weightChannel.valid := io.kernelChannel.valid
    matMuls(batch).outputChannel.ready := io.outputChannel.ready
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

  // all components have the same flow, so if one is ready/valid, then all are ready/valid
  io.outputChannel.valid := matMuls(0).outputChannel.valid
  io.inputChannel.ready := matMuls(0).inputChannel.ready
  io.kernelChannel.ready := matMuls(0).weightChannel.ready

  if (enableDebuggingIO) {
    io.debugInputIm2Col.get := im2colInputs
    io.debugKernel.get := flatKernel
  }
}
