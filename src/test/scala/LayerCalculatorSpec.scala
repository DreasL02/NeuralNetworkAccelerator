
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import utils.MatrixUtils._
import utils.FixedPointConversion._
import utils.RandomData.randomMatrix

class LayerCalculatorSpec extends AnyFreeSpec with ChiselScalatestTester {
  // ======= configure the test =======
  val w = 8
  val wStore = 4 * w
  val xDimension = 3
  val yDimension = 3
  val matrixCommonDimension = 3
  val fixedPoint = 1
  val signed = true.B
  val numberOfTests = 10
  val max = 1.2f
  val min = -1.2f //0.0f //

  val printing = Array.fill(numberOfTests)(false)

  // We can enable printing for a specific test by setting the index to true
  printing(0) = true

  // ======= configure the test end =======

  // --- Rest should be left as is ---
  val seeds = Array.fill(numberOfTests)(0)
  // increment seeds for each test to get different random numbers
  for (i <- 0 until numberOfTests) {
    seeds(i) = i
  }

  // dimensions of the matrices to be multiplied are inferred from the dimensions of the systolic array
  val dimensionOfInputMatrix = Array(yDimension, matrixCommonDimension)
  val dimensionOfWeightMatrix = Array(matrixCommonDimension, xDimension)
  val dimensionOfBiasMatrix = Array(xDimension, yDimension)

  // for each seed, generate a random matrix and test
  for (testNum <- seeds.indices) {
    val enablePrinting = printing(testNum)

    "LayerCalculator should calculate correctly for test %d".format(testNum) in {
      test(new LayerCalculator(w = w, wStore = wStore, xDimension = xDimension, yDimension = yDimension, enableDebuggingIO = true)) { dut =>
        var inputsFloat = randomMatrix(dimensionOfInputMatrix(0), dimensionOfInputMatrix(1), min, max, seeds(testNum))
        var weightsFloat = randomMatrix(dimensionOfWeightMatrix(0), dimensionOfWeightMatrix(1), min, max, seeds(testNum))
        var biasesFloat = randomMatrix(dimensionOfBiasMatrix(0), dimensionOfBiasMatrix(1), min, max, seeds(testNum))

        var multiplicationResultFloat = calculateMatrixMultiplication(inputsFloat, weightsFloat)
        var additionResultFloat = calculateMatrixAddition(multiplicationResultFloat, biasesFloat)
        var reluResultFloat = calculateMatrixReLU(additionResultFloat, signed.litToBoolean)

        if (enablePrinting)
          printMatrixMAC(inputsFloat, weightsFloat, biasesFloat, additionResultFloat, reluResultFloat, "GOLDEN MODEL CALCULATION IN PURE FLOATING")

        val inputsFixed = convertFloatMatrixToFixedMatrix(inputsFloat, fixedPoint, w, signed.litToBoolean)
        val weightsFixed = convertFloatMatrixToFixedMatrix(weightsFloat, fixedPoint, w, signed.litToBoolean)
        val biasesFixed = convertFloatMatrixToFixedMatrix(biasesFloat, fixedPoint * 2, wStore, signed.litToBoolean)
        val biasesFixedForTesting = convertFloatMatrixToFixedMatrix(biasesFloat, fixedPoint, w, signed.litToBoolean)

        val multiplicationResultFixed = calculateMatrixMultiplication(inputsFixed, weightsFixed)
        val additionResultFixed = calculateMatrixAddition(multiplicationResultFixed, biasesFixedForTesting)

        if (enablePrinting)
          printMatrixMAC(inputsFixed, weightsFixed, biasesFixedForTesting, additionResultFixed, "GOLDEN MODEL CALCULATION IN FIXED POINT (NO ReLU)")

        inputsFloat = convertFixedMatrixToFloatMatrix(inputsFixed, fixedPoint, w, signed.litToBoolean)
        weightsFloat = convertFixedMatrixToFloatMatrix(weightsFixed, fixedPoint, w, signed.litToBoolean)
        biasesFloat = convertFixedMatrixToFloatMatrix(biasesFixedForTesting, fixedPoint, w, signed.litToBoolean)

        multiplicationResultFloat = calculateMatrixMultiplication(inputsFloat, weightsFloat)
        additionResultFloat = calculateMatrixAddition(multiplicationResultFloat, biasesFloat)
        reluResultFloat = calculateMatrixReLU(additionResultFloat, signed.litToBoolean)

        if (enablePrinting)
          printMatrixMAC(inputsFloat, weightsFloat, biasesFloat, additionResultFloat, reluResultFloat, "GOLDEN MODEL CALCULATION IN PURE FLOATING")

        // Setup the dut
        dut.io.load.poke(true.B)
        dut.io.signed.poke(signed.asUInt)
        dut.io.fixedPoint.poke(fixedPoint)
        for (i <- inputsFixed.indices) {
          for (j <- inputsFixed(0).indices) {
            dut.io.inputs(i)(j).poke(inputsFixed(i)(xDimension - 1 - j)) // correct format, reverse order in y
            dut.io.weights(i)(j).poke(weightsFixed(yDimension - 1 - j)(i)) // correct format, reverse order in x
            dut.io.biases(i)(j).poke(biasesFixed(i)(j))
          }
        }

        // All values should now be loaded
        dut.clock.step()
        dut.io.load.poke(false.B)
        dut.io.fixedPoint.poke(2.U) // should be ignored when load is false
        dut.io.signed.poke(1.U) // should be ignored when load is false

        var cycles = 0
        while (!dut.io.valid.peekBoolean()) {
          if (enablePrinting) {
            println("Cycle %d".format(cycles))
            println("DEBUG SYSTOLIC ARRAY RESULTS")
            for (i <- 0 until xDimension) {
              for (j <- 0 until yDimension) {
                print(dut.io.debugSystolicArrayResults.get(i)(j).peek().litValue)
                print(" ")
              }
              println()
            }
            println("DEBUG BIASES")
            for (i <- 0 until xDimension) {
              for (j <- 0 until yDimension) {
                print(dut.io.debugBiases.get(i)(j).peek().litValue)
                print(" ")
              }
              println()
            }
            println("ROUNDER INPUTS")
            for (i <- 0 until xDimension) {
              for (j <- 0 until yDimension) {
                print(dut.io.debugRounderInputs.get(i)(j).peek().litValue)
                print(" ")
              }
              println()
            }
            println("ReLU INPUTS")
            for (i <- 0 until xDimension) {
              for (j <- 0 until yDimension) {
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

        val resultFloat = convertFixedMatrixToFloatMatrix(resultFixed, fixedPoint, w, signed.litToBoolean)
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
            if (a - 1 <= b && a + 1 >= b) { //within +-1 of golden model
              valid = true
            }
            assert(valid, ": element at (%d,%d) did not match (got %f : expected %f)".format(i, j, a, b))
          }
        }
      }
    }
  }
}