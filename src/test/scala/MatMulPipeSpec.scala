
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import scala_utils.MatrixUtils._
import scala_utils.FixedPointConversion._
import scala_utils.RandomData.randomMatrix
import TestingUtils.Comparison.CompareWithErrorThreshold
import operators.systolic_array.BufferedSystolicArray

class MatMulPipeSpec extends AnyFreeSpec with ChiselScalatestTester {
  // ======= configure the test =======
  val w = 8
  val wResult = 4 * w
  val numberOfRows = 2
  val numberOfColumns = 3
  val matrixCommonDimension = 4
  val fixedPoint = 2
  val signed = true
  val numberOfTests = 10
  val max = 3.2f
  val min = -3.0f //0.0f //
  val threshold = 1f

  val printing = true
  // ======= configure the test end =======

  // --- Rest should be left as is ---
  val seeds = Array.fill(numberOfTests * 2)(0)
  // increment seeds for each test and matrix to get different random numbers
  for (i <- 0 until numberOfTests * 2) {
    seeds(i) = i
  }

  val inputs = Array.fill(numberOfTests)(Array.ofDim[Float](numberOfRows, matrixCommonDimension))
  for (i <- 0 until numberOfTests) {
    inputs(i) = randomMatrix(numberOfRows, matrixCommonDimension, min, max, seeds(i * 2))
  }

  val weights = Array.fill(numberOfTests)(Array.ofDim[Float](matrixCommonDimension, numberOfColumns))
  for (i <- 0 until numberOfTests) {
    weights(i) = randomMatrix(matrixCommonDimension, numberOfColumns, min, max, seeds(i * 2 + 1))
  }

  val results = Array.fill(numberOfTests)(Array.ofDim[Float](numberOfRows, numberOfColumns))
  for (i <- 0 until numberOfTests) {
    results(i) = calculateMatrixMultiplication(inputs(i), weights(i))
  }

  val inputsFixed = Array.fill(numberOfTests)(Array.ofDim[BigInt](numberOfRows, matrixCommonDimension))
  for (i <- 0 until numberOfTests) {
    inputsFixed(i) = convertFloatMatrixToFixedMatrix(inputs(i), fixedPoint, w, signed)
  }

  val weightsFixed = Array.fill(numberOfTests)(Array.ofDim[BigInt](matrixCommonDimension, numberOfColumns))
  for (i <- 0 until numberOfTests) {
    weightsFixed(i) = convertFloatMatrixToFixedMatrix(weights(i), fixedPoint, w, signed)
  }

  val multiplicationResultFixed = Array.fill(numberOfTests)(Array.ofDim[BigInt](numberOfRows, numberOfColumns))
  for (i <- 0 until numberOfTests) {
    multiplicationResultFixed(i) = calculateMatrixMultiplication(inputsFixed(i), weightsFixed(i))
  }

  val inputsFloat = Array.fill(numberOfTests)(Array.ofDim[Float](numberOfRows, matrixCommonDimension))
  for (i <- 0 until numberOfTests) {
    inputsFloat(i) = convertFixedMatrixToFloatMatrix(inputsFixed(i), fixedPoint, w, signed)
  }
  val weightsFloat = Array.fill(numberOfTests)(Array.ofDim[Float](matrixCommonDimension, numberOfColumns))
  for (i <- 0 until numberOfTests) {
    weightsFloat(i) = convertFixedMatrixToFloatMatrix(weightsFixed(i), fixedPoint, w, signed)
  }
  val multiplicationResultFloat = Array.fill(numberOfTests)(Array.ofDim[Float](numberOfRows, numberOfColumns))
  for (i <- 0 until numberOfTests) {
    multiplicationResultFloat(i) = calculateMatrixMultiplication(inputsFloat(i), weightsFloat(i))
  }

  val resultsFloat = Array.fill(numberOfTests)(Array.ofDim[Float](numberOfRows, numberOfColumns))


  // for each test, generate a random set of matrices and test

  "operators.MatMul should calculate correctly for in pipeline test" in {
    test(new BufferedSystolicArray(w = w, wResult = wResult, numberOfRows = numberOfRows, numberOfColumns = numberOfColumns, commonDimension = matrixCommonDimension, signed = signed, enableDebuggingIO = true)) { dut =>
      var inputNum = 0
      var resultNum = 0
      var cycleTotal = 0

      while (resultNum < numberOfTests) {
        dut.io.inputChannel.valid.poke(true.B)
        dut.io.weightChannel.valid.poke(true.B)
        dut.io.outputChannel.ready.poke(true.B)

        if (dut.io.inputChannel.ready.peek().litToBoolean && inputNum < numberOfTests) {
          if (printing) {
            println("Inputted: ")
            print(matrixToString(inputsFloat(inputNum)))
            println("and ")
            print(matrixToString(weightsFloat(inputNum)))
            println("at cycle: " + cycleTotal)
            println()
          }
          for (i <- 0 until numberOfRows) {
            for (j <- 0 until matrixCommonDimension) {
              dut.io.inputChannel.bits(i)(j).poke(inputsFixed(inputNum)(i)(j).U)
            }
          }
          for (i <- 0 until matrixCommonDimension) {
            for (j <- 0 until numberOfColumns) {
              dut.io.weightChannel.bits(i)(j).poke(weightsFixed(inputNum)(i)(j).U)
            }
          }

          inputNum += 1
        }

        if (dut.io.outputChannel.valid.peek().litToBoolean) {
          for (i <- 0 until numberOfRows) {
            for (j <- 0 until numberOfColumns) {
              val resultFixed = dut.io.outputChannel.bits(i)(j).peek().litValue
              resultsFloat(resultNum)(i)(j) = fixedToFloat(resultFixed, fixedPoint * 2, wResult, signed)
            }
          }

          if (printing) {
            println("Results: ")
            print(matrixToString(resultsFloat(resultNum)))
            println("Expected: ")
            print(matrixToString(multiplicationResultFloat(resultNum)))
            println("at cycle: " + cycleTotal)
            println()
          }

          resultNum += 1
        }

        dut.clock.step()
        cycleTotal += 1

        if (cycleTotal > 1000) {
          fail("Timeout")
        }
      }

      for (i <- 0 until numberOfTests) {
        for (j <- 0 until numberOfRows) {
          for (k <- 0 until numberOfColumns) {
            assert(CompareWithErrorThreshold(resultsFloat(i)(j)(k), multiplicationResultFloat(i)(j)(k), threshold), "Entry " + i + " is not within the threshold. Got: " + resultsFloat(i)(j)(k) + " Expected: " + multiplicationResultFloat(i)(j)(k))
          }
        }
      }
    }
  }
}