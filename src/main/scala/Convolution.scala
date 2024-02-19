import chisel3._
import chisel3.util.log2Ceil
import systolic_array.SystolicArray
import utils.Optional._

// Describe the convolution module using N systolic arrays in parallel to calculate the convolution of an input and a kernel matrix
// The output of each systolic array is then summed and inserted into the output matrix at the correct position
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

// ONNX Convolution Outputs:
// Y: Tensor - the output tensor, a 4D tensor in the format [batch, output_channel, output_height, output_width]

// ONNX Convolution Pseudo Code:
// Y[b, o, x, y] = sum(i, kx, ky) (W[o, i, kx, ky] * X[b, i, x * stride_x + kx * dilation_x - pad_x, y * stride_y + ky * dilation_y - pad_y]) + B[o]
// So to model this in Chisel, we need to calculate the convolution of X and W, and then add B to the result
// We can use a series of systolic arrays to calculate the convolution, and then use a series of adders to add B to the result
// As each systolic arrays calculates the convolution of a single channel,
// we can use N systolic arrays in parallel to calculate the matrix multiplication of X and W for each channel
// and then sum the results to get the final output matrix


class Convolution(w: Int = 8, wStore: Int = 32,
                  numberOfChannels: Int = 3, // the number of channels in the input matrix
                  inputDimensions: (Int, Int) = (32, 32), // the dimensions of the input matrix
                  kernelDimensions: (Int, Int) = (3, 3), // the dimensions of the kernel matrix


                 ) extends Module {
  val io = IO(new Bundle {
    val X = Input(Vec(numberOfChannels, Vec(inputDimensions._1, Vec(inputDimensions._2, UInt(w.W))))) // the input matrix
    val W = Input(Vec(numberOfChannels, Vec(kernelDimensions._1, Vec(kernelDimensions._2, UInt(w.W))))) // the kernel matrix
    val B = Input(Vec(numberOfChannels, UInt(wStore.W))) // the bias matrix
  })


  val systolicArrays = for (i <- 0 until numberOfChannels) yield { // create array of systolic arrays for each channel
    val systolicArray = Module(new SystolicArray(w, wStore, 4, 4)) // create systolic array
    systolicArray // return module
  }

  // For each systolic array, connect the inputs and weights to the input matrix and kernel matrix
  for (i <- 0 until numberOfChannels) {
    systolicArrays(i).io.a := io.X(i)
    systolicArrays(i).io.b := io.W(i)


  }
}
