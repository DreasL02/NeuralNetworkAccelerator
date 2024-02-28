import chisel3._
import chisel3.util.log2Ceil
import systolic_array.SystolicArray
import scala_utils.Optional._

// Describe the convolution module using N element wise multipliers in parallel
// to calculate the convolution of an input and a kernel matrix

// The arguments are as follows:
// w: Int - the bit width of the input and kernel matrices
// wStore: Int - the bit width of the output matrix

// ONNX Convolution Attributes:
// auto_pad: String - the padding mode to use, can be "NOTSET", "SAME_UPPER", "SAME_LOWER", or "VALID"
// - if auto_pad is "NOTSET", then the pads attribute will be used
// - if auto_pad is "SAME_UPPER" or "SAME_LOWER", then the pads attribute will be calculated e.g. for SAME_UPPER:
//    pad_x = ceil(input_height * stride_x - input_height + kernel_height - 1) / 2
//    pad_y = ceil(input_width * stride_y - input_width + kernel_width - 1) / 2
//  and for SAME_LOWER:
//    pad_x = floor(input_height * stride_x - input_height + kernel_height - 1) / 2
//    pad_y = floor(input_width * stride_y - input_width + kernel_width - 1) / 2
// - if auto_pad is "VALID", then no padding will be used
// This should probably be handled in python?


// dilations: Seq[Int] - the dilation to use for the convolution e.g how many pixels to skip when applying the kernel
// group: Int - the number of groups to use for the convolution e.g. if group is 2, then the input and kernel will be
//                                     split into 2 groups and the convolution will be done separately for each group
// kernel_shape: Seq[Int] - the shape of the kernel

// pads: Seq[Int] - the padding to use for the convolution
// strides: Seq[Int] - the stride to use for the convolution

// ONNX Convolution Inputs:
// X: Tensor - the input tensor, a 4D tensor in the format [batch, input_channel, input_height, input_width]
// W: Tensor - the kernel tensor, a 4D tensor in the format [output_channel, input_channel, kernel_height, kernel_width]
// B: Tensor - the bias tensor, a 1D tensor in the format [output_channel]

// NB: not much documentation on the bias tensor, and as it is optional, it is not included in this module

// batch size is assumed to be 1 always, i.e. only one image is processed at a time

// ONNX Convolution Outputs:
// Y: Tensor - the output tensor, a 4D tensor in the format [batch, output_channel, output_height, output_width]

// ONNX Convolution Pseudo Code:
// Y[b, o, x, y] = sum(i, kx, ky) (W[o, i, kx, ky] * X[b, i, x * stride_x + kx * dilation_x - pad_x, y * stride_y + ky * dilation_y - pad_y]) + B[o]


// this module will only do convolution for a single channel
class SingleChannelConvolution(w: Int = 8,
                               wBig: Int = 32,
                               inputDimensions: (Int, Int) = (32, 32), // the dimensions of the input matrix
                               kernelDimensions: (Int, Int) = (3, 3), // the dimensions of the kernel matrix
                               diliations: (Int, Int) = (1, 1), // the diliations to use for the convolution
                               strides: (Int, Int) = (1, 1), // the stride to use for the convolution
                               pads: (Int, Int) = (0, 0) // the padding to use for the convolution
                              ) extends Module {
  val io = IO(new Bundle {
    val X = Input(Vec(inputDimensions._1, Vec(inputDimensions._2, UInt(w.W)))) // the input matrix
    val W = Input(Vec(kernelDimensions._1, Vec(kernelDimensions._2, UInt(w.W)))) // the kernel matrix

    val Y = Output(Vec(inputDimensions._1, Vec(inputDimensions._2, UInt(wBig.W)))) // the output matrix

  })
  /*

  val numberOfSlices = inputDimensions._1 * inputDimensions._2


  // TODO: factor in the padding and stride
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


  // TODO: factor in dilation
  val elementWiseMultipliers = for (i <- 0 until inputDimensions._1) yield {
    for (j <- 0 until inputDimensions._2) yield {
      val elementWiseMultiplier = Module(new ElementWiseMultiplier(w, wBig, kernelDimensions._1, kernelDimensions._2))
      elementWiseMultiplier.io.inputs := inputSlices(i)(j)
      elementWiseMultiplier.io.weights := io.W
      elementWiseMultiplier.io.result
    }
  }

  // sum the results of the element wise multipliers
  val summer = for (i <- 0 until numberOfSlices) yield {
    val summer = Module(new Summer(wBig, kernelDimensions._1, kernelDimensions._2))
    summer.io.inputs := elementWiseMultipliers(i)
    summer.io.result
  }

  val result = Wire(Vec(inputDimensions._1, Vec(inputDimensions._2, UInt(wBig.W))))
  for (i <- 0 until inputDimensions._1) {
    for (j <- 0 until inputDimensions._2) {
      result(i)(j) := summer(i * inputDimensions._2 + j)
    }
  }


  io.Y := result

   */
}
