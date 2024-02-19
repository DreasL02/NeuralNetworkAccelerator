import chisel3._
import chisel3.util.log2Ceil
import systolic_array.SystolicArray
import utils.Optional._

// Describe the convolution module using N element wise multipliers in parallel
// to calculate the convolution of an input and a kernel matrix

// The arguments are as follows:
// w: Int - the bit width of the input and kernel matrices
// wStore: Int - the bit width of the output matrix

// ONNX Convolution Attributes:
// auto_pad: String - the padding mode to use, can be "NOTSET", "SAME_UPPER", "SAME_LOWER", or "VALID"
// dilations: Seq[Int] - the dilation to use for the convolution
// group: Int - the number of groups to use for the convolution
// kernel_shape: Seq[Int] - the shape of the kernel
// pads: Seq[Int] - the padding to use for the convolution
// strides: Seq[Int] - the stride to use for the convolution

// ONNX Convolution Inputs:
// X: Tensor - the input tensor, a 4D tensor in the format [batch, input_channel, input_height, input_width]
// W: Tensor - the kernel tensor, a 4D tensor in the format [output_channel, input_channel, kernel_height, kernel_width]
// B: Tensor - the bias tensor, a 1D tensor in the format [output_channel]

// batch size is assumed to be 1 always, i.e. only one image is processed at a time

// ONNX Convolution Outputs:
// Y: Tensor - the output tensor, a 4D tensor in the format [batch, output_channel, output_height, output_width]

// ONNX Convolution Pseudo Code:
// Y[b, o, x, y] = sum(i, kx, ky) (W[o, i, kx, ky] * X[b, i, x * stride_x + kx * dilation_x - pad_x, y * stride_y + ky * dilation_y - pad_y]) + B[o]


// this module will only do convolution for a single channel
class Convolution(w: Int = 8, wBig: Int = 32,
                  inputDimensions: (Int, Int) = (32, 32), // the dimensions of the input matrix
                  kernelDimensions: (Int, Int) = (3, 3), // the dimensions of the kernel matrix
                  diliations: (Int, Int) = (1, 1), // the diliations to use for the convolution
                  group: Int = 1, // the number of groups to use for the convolution
                  strides: (Int, Int) = (1, 1), // the stride to use for the convolution
                  pads: (Int, Int, Int, Int) = (0, 0, 0, 0) // the padding to use for the convolution
                 ) extends Module {
  val io = IO(new Bundle {
    val X = Input(Vec(inputDimensions._1, Vec(inputDimensions._2, UInt(w.W)))) // the input matrix
    val W = Input(Vec(kernelDimensions._1, Vec(kernelDimensions._2, UInt(w.W)))) // the kernel matrix
    val B = Input(UInt(wBig.W)) // the bias value

    val Y = Output(Vec(inputDimensions._1, Vec(inputDimensions._2, UInt(wBig.W)))) // the output matrix

  })

  val inputSlices = for (i <- 0 until inputDimensions._1) yield {
    for (j <- 0 until inputDimensions._2) yield {
      val slice = Wire(Vec(kernelDimensions._1, Vec(kernelDimensions._2, UInt(w.W)))
      )
      for (k <- 0 until kernelDimensions._1) {
        for (l <- 0 until kernelDimensions._2) {
          slice(k)(l) := io.X(i + k)(j + l)
        }
      }
      slice
    }
  }

  val elementWiseMultipliers = for (i <- 0 until inputDimensions._1) yield {
    for (j <- 0 until inputDimensions._2) yield {
      val elementWiseMultiplier = Module(new ElementWiseMultiplier(w, wBig, kernelDimensions._1, kernelDimensions._2))
      elementWiseMultiplier.io.inputs := inputSlices(i)(j)
      elementWiseMultiplier.io.weights := io.W
      elementWiseMultiplier.io.result
    }
  }

  val summer = for (i <- 0 until inputDimensions._1) yield {
    val summer = Module(new Summer(wBig, inputDimensions._1, inputDimensions._2))
    summer.io.inputs := elementWiseMultipliers(i)
    summer.io.result
  }
}
