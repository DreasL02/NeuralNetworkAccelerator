import chisel3._
import chisel3.util.DecoupledIO
import scala_utils.Optional.optional

class ConvMatMul(
                  w: Int = 8,
                  wResult: Int = 32,
                  inputDimensions: (Int, Int) = (32, 32), // the dimensions of the input matrix
                  kernelDimensions: (Int, Int) = (3, 3), // the dimensions of the kernel matrix
                  signed: Boolean = true, // whether the input and kernel matrices are signed
                  strides: (Int, Int) = (1, 1), // the stride to use for the convolution
                  pads: (Int, Int) = (0, 0), // the padding to use for the convolution
                  enableDebuggingIO: Boolean = true
                ) extends Module {

  private val outputDimensions = ((inputDimensions._1 - kernelDimensions._1 + 2 * pads._1) / strides._1 + 1, (inputDimensions._2 - kernelDimensions._2 + 2 * pads._2) / strides._2 + 1)
  private val paddedDimensions = (inputDimensions._1 + 2 * pads._1, inputDimensions._2 + 2 * pads._2)
  private val numberOfToeplitzMatrices = paddedDimensions._1 // number of rows in the padded kernel matrix
  private val toeplitzMatrixDimensions = (paddedDimensions._1 - kernelDimensions._1 + 1, paddedDimensions._2 - kernelDimensions._2 + 1)
  private val doublyBlockedToeplitzDimensions = (toeplitzMatrixDimensions._1 * toeplitzMatrixDimensions._2, paddedDimensions._1 * paddedDimensions._2)

  println("outputDimensions: " + outputDimensions)
  println("paddedDimensions: " + paddedDimensions)
  println("numberOfToeplitzMatrices: " + numberOfToeplitzMatrices)
  println("toeplitzMatrixDimensions: " + toeplitzMatrixDimensions)
  println("doublyBlockedToeplitzDimensions: " + doublyBlockedToeplitzDimensions)

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(inputDimensions._1, Vec(inputDimensions._2, UInt(w.W)))))
    val kernelChannel = Flipped(new DecoupledIO(Vec(kernelDimensions._1, Vec(kernelDimensions._2, UInt(w.W)))))

    val outputChannel = new DecoupledIO(Vec(outputDimensions._1, Vec(outputDimensions._2, UInt(wResult.W))))

    val debugPaddedInput = optional(enableDebuggingIO, Output(Vec(paddedDimensions._1, Vec(paddedDimensions._2, UInt(w.W)))))
    val debugZeroPaddedKernel = optional(enableDebuggingIO, Output(Vec(paddedDimensions._1, Vec(paddedDimensions._2, UInt(w.W)))))
    val debugToeplitzMatrices = optional(enableDebuggingIO, Output(Vec(numberOfToeplitzMatrices, Vec(toeplitzMatrixDimensions._1, Vec(toeplitzMatrixDimensions._2, UInt(w.W))))))
    val debugDoublyBlockedToeplitzMatrix = optional(enableDebuggingIO, Output(Vec(doublyBlockedToeplitzDimensions._1, Vec(doublyBlockedToeplitzDimensions._2, UInt(w.W)))))
    val debugColumnInput = optional(enableDebuggingIO, Output(Vec(1, Vec(paddedDimensions._1 * paddedDimensions._2, UInt(w.W)))))
    val debugMatMul = optional(enableDebuggingIO, Output(Vec(doublyBlockedToeplitzDimensions._1, Vec(1, UInt(wResult.W)))))
  })

  // https://stackoverflow.com/questions/16798888/2-d-convolution-as-a-matrix-matrix-multiplication

  private val paddedInput = Wire(Vec(paddedDimensions._1, Vec(paddedDimensions._2, UInt(w.W))))
  for (i <- 0 until paddedDimensions._1) {
    for (j <- 0 until paddedDimensions._2) {
      if (i < pads._1 || i >= inputDimensions._1 + pads._1 || j < pads._2 || j >= inputDimensions._2 + pads._2) {
        paddedInput(i)(j) := 0.U
      } else {
        paddedInput(i)(j) := io.inputChannel.bits(i - pads._1)(j - pads._2)
      }
    }
  }

  // 3- Zero-pad the filter matrix
  // e.g
  // 10 20 -> 0  0  0 0
  // 30 40    10 20 0 0
  //          30 40 0 0
  // For a 2x2 kernel and a 4x4 input

  val zeroPaddedKernel = Wire(Vec(paddedDimensions._1, Vec(paddedDimensions._2, UInt(w.W))))
  for (i <- 0 until paddedDimensions._1) {
    for (j <- 0 until paddedDimensions._2) {
      if (i < kernelDimensions._1 || i >= kernelDimensions._1 + kernelDimensions._1 || j < kernelDimensions._2 || j >= kernelDimensions._2 + kernelDimensions._2) {
        zeroPaddedKernel(i)(j) := 0.U
      } else {
        zeroPaddedKernel(i)(j) := io.kernelChannel.bits(i - kernelDimensions._1)(j - kernelDimensions._2)
      }
    }
  }
  if (enableDebuggingIO) {
    io.debugPaddedInput.get := paddedInput
    io.debugZeroPaddedKernel.get := zeroPaddedKernel
  }

  // 4- Create Toeplitz matrix for each row of the zero-padded filter
  val toeplitzMatrices = for (i <- 0 until numberOfToeplitzMatrices) yield {
    val toeplitzMatrix = Wire(Vec(toeplitzMatrixDimensions._1, Vec(toeplitzMatrixDimensions._2, UInt(w.W))))
    for (j <- 0 until toeplitzMatrixDimensions._1) {
      for (k <- 0 until toeplitzMatrixDimensions._2) {
        toeplitzMatrix(j)(k) := paddedInput(j + i / paddedDimensions._1)(k + i % paddedDimensions._2)
      }
    }
    toeplitzMatrix
  }
  if (enableDebuggingIO) {
    io.debugToeplitzMatrices.get := toeplitzMatrices
  }

  // 5- Create a doubly blocked Toeplitz matrix
  val doublyBlockedToeplitzMatrix = Wire(Vec(doublyBlockedToeplitzDimensions._1, Vec(doublyBlockedToeplitzDimensions._2, UInt(w.W))))
  for (i <- 0 until toeplitzMatrixDimensions._1) {
    for (j <- 0 until toeplitzMatrixDimensions._2) {
      for (k <- 0 until paddedDimensions._1) {
        for (l <- 0 until paddedDimensions._2) {
          doublyBlockedToeplitzMatrix(i * toeplitzMatrixDimensions._1 + j)(k * paddedDimensions._1 + l) := toeplitzMatrices(i)(j)(k)(l)
        }
      }
    }
  }
  if (enableDebuggingIO) {
    io.debugDoublyBlockedToeplitzMatrix.get := doublyBlockedToeplitzMatrix
  }

  // 6- Convert the input matrix to a column vector
  val columnInput = Wire(Vec(1, Vec(paddedDimensions._1 * paddedDimensions._2, UInt(w.W))))
  for (i <- 0 until paddedDimensions._1) {
    for (j <- 0 until paddedDimensions._2) {
      columnInput(0)(i * paddedDimensions._1 + j) := paddedInput(i)(j)
    }
  }
  if (enableDebuggingIO) {
    io.debugColumnInput.get := columnInput
  }

  // 7  Multiply doubly blocked toeplitz matrix with vectorized input signal (using MatMul-node)
  val matmul = Module(new MatMul(
    w, wResult,
    doublyBlockedToeplitzDimensions._1, // number of rows in the result matrix / number of rows in the first matrix
    1, // number of columns in the result matrix / number of columns in the second matrix
    doublyBlockedToeplitzDimensions._2, // number of columns in the first matrix and number of rows in the second matrix
    signed,
    enableDebuggingIO
  ))
  // input and weight channels are flipped in the MatMul module
  matmul.io.inputChannel.bits := doublyBlockedToeplitzMatrix
  matmul.io.weightChannel.bits := columnInput

  matmul.io.inputChannel.valid := io.inputChannel.valid
  matmul.io.weightChannel.valid := io.kernelChannel.valid

  matmul.io.resultChannel.ready := io.outputChannel.ready

  if (enableDebuggingIO) {
    io.debugMatMul.get := matmul.io.resultChannel.bits
  }


  // 8- Reshape the result to the output matrix
  val reshapedResult = Wire(Vec(outputDimensions._1, Vec(outputDimensions._2, UInt(wResult.W))))
  for (i <- 0 until outputDimensions._1) {
    for (j <- 0 until outputDimensions._2) {
      reshapedResult(i)(j) := matmul.io.resultChannel.bits(i * outputDimensions._1 + j)
    }
  }

  io.outputChannel.bits := reshapedResult
  io.outputChannel.valid := matmul.io.resultChannel.valid
}
