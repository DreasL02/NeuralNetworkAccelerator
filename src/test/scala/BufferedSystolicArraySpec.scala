
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import scala_utils.MatrixUtils._
import scala_utils.FixedPointConversion._
import scala_utils.RandomData.randomMatrix
import systolic_array.BufferedSystolicArray

class BufferedSystolicArraySpec extends AnyFreeSpec with ChiselScalatestTester {
  // ======= configure the test =======
  val w = 8
  val wResult = 4 * w
  val numberOfRows = 1
  val numberOfColumns = 16
  val matrixCommonDimension = 1
  val fixedPoint = 2
  val signed = true
  val numberOfTests = 10
  val max = 3.2f
  val min = -3.0f //0.0f //
  val threshold = 1f

  val printing = Array.fill(numberOfTests)(false)

  // We can enable printing for a specific test by setting the index to true
  printing(0) = true


  // ======= configure the test end =======

  // --- Rest should be left as is ---
  val seeds = Array.fill(numberOfTests * 2)(0)
  // increment seeds for each test and matrix to get different random numbers
  for (i <- 0 until numberOfTests * 2) {
    seeds(i) = i
  }

  // for each test, generate a random set of matrices and test
  for (testNum <- 0 until numberOfTests) {
    val enablePrinting = printing(testNum)

    "BufferedSystolicArray should calculate correctly for test %d".format(testNum) in {
      test(new BufferedSystolicArray(w = w, wResult = wResult, numberOfRows = numberOfRows, numberOfColumns = numberOfColumns, commonDimension = matrixCommonDimension, signed = signed, enableDebuggingIO = true)) { dut =>
        var inputsFloat = randomMatrix(numberOfRows, matrixCommonDimension, min, max, seeds(testNum * 2))
        var weightsFloat = randomMatrix(matrixCommonDimension, numberOfColumns, min, max, seeds(testNum * 2 + 1))

        var multiplicationResultFloat = calculateMatrixMultiplication(inputsFloat, weightsFloat)

        if (enablePrinting)
          printMatrixMultiplication(inputsFloat, weightsFloat, multiplicationResultFloat, "GOLDEN MODEL CALCULATION IN PURE FLOATING")

        val inputsFixed = convertFloatMatrixToFixedMatrix(inputsFloat, fixedPoint, w, signed)
        val weightsFixed = convertFloatMatrixToFixedMatrix(weightsFloat, fixedPoint, w, signed)

        val multiplicationResultFixed = calculateMatrixMultiplication(inputsFixed, weightsFixed)

        if (enablePrinting)
          printMatrixMultiplication(inputsFixed, weightsFixed, multiplicationResultFixed, "GOLDEN MODEL CALCULATION IN PURE FIXED")

        inputsFloat = convertFixedMatrixToFloatMatrix(inputsFixed, fixedPoint, w, signed)
        weightsFloat = convertFixedMatrixToFloatMatrix(weightsFixed, fixedPoint, w, signed)

        multiplicationResultFloat = calculateMatrixMultiplication(inputsFloat, weightsFloat)

        if (enablePrinting)
          printMatrixMultiplication(inputsFloat, weightsFloat, multiplicationResultFloat, "GOLDEN MODEL CALCULATION IN RECONVERTED FLOATING")


        val formattedInputs = inputsFixed
        // input rows need to be reversed
        for (i <- formattedInputs.indices) {
          formattedInputs(i) = formattedInputs(i).reverse
        }

        // Weights need to be transposed
        val formattedWeights = weightsFixed.transpose
        // and rows need to be reversed
        for (i <- formattedWeights.indices) {
          formattedWeights(i) = formattedWeights(i).reverse
        }

        if (enablePrinting) {
          println("FORMATTED INPUTS")
          print(matrixToString(formattedInputs))
          println("FORMATTED WEIGHTS")
          print(matrixToString(formattedWeights))
        }

        // Setup the dut by loading the inputs and weights into the buffers
        dut.io.load.poke(true.B)

        for (i <- 0 until numberOfRows) {
          for (j <- 0 until matrixCommonDimension) {
            dut.io.inputs(i)(j).poke(formattedInputs(i)(j))
          }
        }

        for (i <- 0 until numberOfColumns) {
          for (j <- 0 until matrixCommonDimension) {
            dut.io.weights(i)(j).poke(formattedWeights(i)(j))
          }
        }

        dut.clock.step()
        // All values should now be loaded into the buffers
        dut.io.load.poke(false.B)

        // Wait for the systolic array to be done
        var cycles = 0
        while (!dut.io.valid.peekBoolean()) {
          if (enablePrinting) {
            println("Cycle %d".format(cycles))
            println("DEBUG WEIGHTS / DEBUG INPUTS")
            print("  ")
            for (i <- 0 until numberOfColumns) {
              print(dut.io.debugWeights.get(i).peek().litValue)
              print(" ")
            }
            println()
            for (i <- 0 until numberOfRows) {
              print(dut.io.debugInputs.get(i).peek().litValue)
              println()
            }
            println()
            println("DEBUG SYSTOLIC ARRAY RESULTS")
            for (i <- 0 until numberOfRows) {
              for (j <- 0 until numberOfColumns) {
                print(dut.io.debugSystolicArrayResults.get(i)(j).peek().litValue)
                print(" ")
              }
              println()
            }
          }
          cycles = cycles + 1
          dut.clock.step()
        }


        val resultFixed: Array[Array[BigInt]] = Array.fill(multiplicationResultFixed.length, multiplicationResultFixed(0).length)(0)
        for (i <- multiplicationResultFixed.indices) {
          for (j <- multiplicationResultFixed(0).indices) {
            resultFixed(i)(j) = dut.io.result(i)(j).peek().litValue
          }
        }

        val resultFloat = convertFixedMatrixToFloatMatrix(resultFixed, fixedPoint * 2, w * 2, signed)
        if (enablePrinting) {
          println("DONE IN %d CYCLES".format(cycles))
          println("RESULT IN FLOATING POINT")
          print(matrixToString(resultFloat))
          println("RESULT IN FIXED POINT")
          print(matrixToString(resultFixed))
        }

        // Evaluate
        for (i <- multiplicationResultFloat.indices) {
          for (j <- multiplicationResultFloat(0).indices) {
            val a = resultFloat(i)(j)
            val b = multiplicationResultFloat(i)(j)
            var valid = false
            if (a - threshold <= b && a + threshold >= b) { //within +-threshold of golden model
              valid = true
            }
            assert(valid, ": element at (%d,%d) did not match (got %f : expected %f)".format(i, j, a, b))
          }
        }
      }
    }
  }
}