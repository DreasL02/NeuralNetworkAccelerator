
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import scala_utils.MatrixUtils._
import scala_utils.FixedPointConversion._
import scala_utils.RandomData.randomMatrix

class LayerCalculatorSpec extends AnyFreeSpec with ChiselScalatestTester {
  /*
  // ======= configure the test =======
  val w = 8
  val wResult = 4 * w
  val numberOfRows = 3
  val numberOfColumns = 2
  val commonDimension = 5
  val fixedPoint = 2
  val signed = true
  val numberOfTests = 1
  val max = 3.2f
  val min = -3.0f //0.0f //
  val threshold = 1f

  val printing = Array.fill(numberOfTests)(false)

  // We can enable printing for a specific test by setting the index to true
  printing(0) = true

  // ======= configure the test end =======

  // --- Rest should be left as is ---
  val seeds = Array.fill(numberOfTests * 3)(0)
  // increment seeds for each test and matrix to get different random numbers
  for (i <- 0 until numberOfTests * 3) {
    seeds(i) = i
  }

  // for each test, generate a random set of matrices and test
  for (testNum <- 0 until numberOfTests) {
    val enablePrinting = printing(testNum)

    "LayerCalculator should calculate correctly for test %d".format(testNum) in {
      test(new LayerCalculator(w = w, wResult = wResult, numberOfRows = numberOfRows, numberOfColumns = numberOfColumns, commonDimension = commonDimension, signed = signed, fixedPoint = fixedPoint, enableDebuggingIO = true)) { dut =>
        var inputsFloat = randomMatrix(numberOfRows, commonDimension, min, max, seeds(testNum * 3))
        var weightsFloat = randomMatrix(commonDimension, numberOfColumns, min, max, seeds(testNum * 3 + 1))
        var biasesFloat = randomMatrix(numberOfRows, numberOfColumns, min, max, seeds(testNum * 3 + 2))

        var multiplicationResultFloat = calculateMatrixMultiplication(inputsFloat, weightsFloat)
        var additionResultFloat = calculateMatrixAddition(multiplicationResultFloat, biasesFloat)
        var reluResultFloat = calculateMatrixReLU(additionResultFloat, signed)

        if (enablePrinting)
          printMatrixMAC(inputsFloat, weightsFloat, biasesFloat, additionResultFloat, reluResultFloat, "GOLDEN MODEL CALCULATION IN PURE FLOATING")

        val inputsFixed = convertFloatMatrixToFixedMatrix(inputsFloat, fixedPoint, w, signed)
        val weightsFixed = convertFloatMatrixToFixedMatrix(weightsFloat, fixedPoint, w, signed)
        val biasesFixed = convertFloatMatrixToFixedMatrix(biasesFloat, fixedPoint * 2, wResult, signed)
        val biasesFixedForTesting = convertFloatMatrixToFixedMatrix(biasesFloat, fixedPoint, w, signed)
        val multiplicationResultFixed = calculateMatrixMultiplication(inputsFixed, weightsFixed)
        val additionResultFixed = calculateMatrixAddition(multiplicationResultFixed, biasesFixedForTesting)

        if (enablePrinting)
          printMatrixMAC(inputsFixed, weightsFixed, biasesFixedForTesting, additionResultFixed, "GOLDEN MODEL CALCULATION IN FIXED POINT (NO ReLU)")

        inputsFloat = convertFixedMatrixToFloatMatrix(inputsFixed, fixedPoint, w, signed)
        weightsFloat = convertFixedMatrixToFloatMatrix(weightsFixed, fixedPoint, w, signed)
        biasesFloat = convertFixedMatrixToFloatMatrix(biasesFixedForTesting, fixedPoint, w, signed)

        multiplicationResultFloat = calculateMatrixMultiplication(inputsFloat, weightsFloat)
        additionResultFloat = calculateMatrixAddition(multiplicationResultFloat, biasesFloat)
        reluResultFloat = calculateMatrixReLU(additionResultFloat, signed)

        if (enablePrinting)
          printMatrixMAC(inputsFloat, weightsFloat, biasesFloat, additionResultFloat, reluResultFloat, "GOLDEN MODEL CALCULATION IN PURE FLOATING")

        if (enablePrinting) {
          println("FORMATTED INPUTS")
          print(matrixToString(inputsFixed))
          println("FORMATTED WEIGHTS")
          print(matrixToString(weightsFixed))
        }

        // Setup the dut by indicating that the producer is ready
        dut.io.ready.poke(true.B)

        for (i <- 0 until numberOfRows) {
          for (j <- 0 until commonDimension) {
            dut.io.inputs(i)(j).poke(inputsFixed(i)(j))
          }
        }

        for (i <- 0 until commonDimension) {
          for (j <- 0 until numberOfColumns) {
            dut.io.weights(i)(j).poke(weightsFixed(i)(j))
          }
        }

        for (i <- biasesFixed.indices) {
          for (j <- biasesFixed(0).indices) {
            dut.io.biases(i)(j).poke(biasesFixed(i)(j))
          }
        }

        dut.clock.step()
        // All values should now be loaded

        var cycles = 0
        while (!dut.io.valid.peekBoolean()) {
          if (enablePrinting) {
            println("Cycle %d".format(cycles))
            println("DEBUG SYSTOLIC ARRAY RESULTS")
            for (i <- 0 until numberOfRows) {
              for (j <- 0 until numberOfColumns) {
                print(dut.io.debugMatMulResults.get(i)(j).peek().litValue)
                print(" ")
              }
              println()
            }
            println("DEBUG BIASES")
            for (i <- 0 until numberOfRows) {
              for (j <- 0 until numberOfColumns) {
                print(dut.io.debugBiases.get(i)(j).peek().litValue)
                print(" ")
              }
              println()
            }
            println("ROUNDER INPUTS")
            for (i <- 0 until numberOfRows) {
              for (j <- 0 until numberOfColumns) {
                print(dut.io.debugRounderInputs.get(i)(j).peek().litValue)
                print(" ")
              }
              println()
            }
            println("ReLU INPUTS")
            for (i <- 0 until numberOfRows) {
              for (j <- 0 until numberOfColumns) {
                print(dut.io.debugReLUInputs.get(i)(j).peek().litValue)
                print(" ")
              }
              println()
            }
            println()
          }
          cycles = cycles + 1
          dut.clock.step()
        }


        val resultFixed: Array[Array[BigInt]] = Array.fill(additionResultFixed.length, additionResultFixed(0).length)(0)
        for (i <- additionResultFixed.indices) {
          for (j <- additionResultFixed(0).indices) {
            resultFixed(i)(j) = dut.io.result(i)(j).peek().litValue
          }
        }

        val resultFloat = convertFixedMatrixToFloatMatrix(resultFixed, fixedPoint, w, signed)
        if (enablePrinting) {
          println("DONE IN %d CYCLES".format(cycles))
          println("RESULT IN FLOATING POINT")
          print(matrixToString(resultFloat))
          println("RESULT IN FIXED POINT")
          print(matrixToString(resultFixed))
        }

        // Evaluate
        for (i <- reluResultFloat.indices) {
          for (j <- reluResultFloat(0).indices) {
            val a = resultFloat(i)(j)
            val b = reluResultFloat(i)(j)
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

   */
}