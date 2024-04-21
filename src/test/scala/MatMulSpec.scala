
import chisel3._
import chiseltest._
import operators.MatMul
import org.scalatest.freespec.AnyFreeSpec
import scala_utils.MatrixUtils._
import scala_utils.FixedPointConversion._
import scala_utils.RandomData.randomMatrix

class MatMulSpec extends AnyFreeSpec with ChiselScalatestTester {
  // ======= configure the test =======
  val w = 8
  val wResult = 4 * w
  val numberOfRows = 2
  val numberOfColumns = 3
  val matrixCommonDimension = 4
  val fixedPoint = 2
  val signed = true
  val numberOfTests = 1
  val max = 3.2f
  val min = -3.0f //0.0f //
  val threshold = 1f

  val printing = Array.fill(numberOfTests)(false)
  val printWeightsAndInputs = false // only supported for systolic array
  val printInterfaces = true
  val printCounters = true // only supported for one at a time matrix multiplication

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

    "operators.MatMul should calculate correctly for test %d".format(testNum) in {
      test(new MatMul(w = w, wResult = wResult, numberOfRows = numberOfRows, numberOfColumns = numberOfColumns, commonDimension = matrixCommonDimension, signed = signed, enableDebuggingIO = true)) { dut =>
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

        if (enablePrinting) {
          println("FORMATTED INPUTS")
          print(matrixToString(inputsFixed))
          println("FORMATTED WEIGHTS")
          print(matrixToString(weightsFixed))
        }

        if (printInterfaces) {
          println("Interfaces at beginning:")
          println("Input Channel Valid: %b".format(dut.io.inputChannel.valid.peek().litToBoolean))
          println("Input Channel Ready: %b".format(dut.io.inputChannel.ready.peek().litToBoolean))
          println("Weight Channel Valid: %b".format(dut.io.weightChannel.valid.peek().litToBoolean))
          println("Weight Channel Ready: %b".format(dut.io.weightChannel.ready.peek().litToBoolean))
          println("Result Channel Valid: %b".format(dut.io.outputChannel.valid.peek().litToBoolean))
          println("Result Channel Ready: %b".format(dut.io.outputChannel.ready.peek().litToBoolean))
          println("DEBUG COMPUTATION START: %b".format(dut.io.debugComputationStart.get.peek().litToBoolean))
          println()
        }

        // Setup the dut by loading the inputs and weights and indicating that they are valid
        for (i <- 0 until numberOfRows) {
          for (j <- 0 until matrixCommonDimension) {
            dut.io.inputChannel.bits(i)(j).poke(inputsFixed(i)(j))
          }
        }
        dut.io.inputChannel.valid.poke(true.B)

        for (i <- 0 until matrixCommonDimension) {
          for (j <- 0 until numberOfColumns) {
            dut.io.weightChannel.bits(i)(j).poke(weightsFixed(i)(j))
          }
        }
        dut.io.weightChannel.valid.poke(true.B)

        dut.io.outputChannel.ready.poke(true.B)

        // Wait for the systolic array to be done
        var cycles = 0
        while (!dut.io.outputChannel.valid.peekBoolean()) {
          if (enablePrinting) {
            println("Cycle %d".format(cycles))
            println()
            if (printInterfaces) {
              println("Interfaces:")
              println("Input Channel Valid: %b".format(dut.io.inputChannel.valid.peek().litToBoolean))
              println("Input Channel Ready: %b".format(dut.io.inputChannel.ready.peek().litToBoolean))
              println("Weight Channel Valid: %b".format(dut.io.weightChannel.valid.peek().litToBoolean))
              println("Weight Channel Ready: %b".format(dut.io.weightChannel.ready.peek().litToBoolean))
              println("Result Channel Valid: %b".format(dut.io.outputChannel.valid.peek().litToBoolean))
              println("Result Channel Ready: %b".format(dut.io.outputChannel.ready.peek().litToBoolean))
              println("DEBUG COMPUTATION START: %b".format(dut.io.debugComputationStart.get.peek().litToBoolean))
              println()
            }
            if (printWeightsAndInputs) {
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
            }

            if (printCounters) {
              println("COUNTERS")
              println("Row Counter: %d".format(dut.io.debugCounters.get(0).peek().litValue))
              println("Column Counter: %d".format(dut.io.debugCounters.get(1).peek().litValue))
              println("Common Counter: %d".format(dut.io.debugCounters.get(2).peek().litValue))
              println()
              println("CYCLE INPUTS")
              println("Cycle Input: %f".format(fixedToFloat(dut.io.debugCycleInputs.get(0).peek().litValue, fixedPoint, w, signed)))
              println("Cycle Weight: %f".format(fixedToFloat(dut.io.debugCycleInputs.get(1).peek().litValue, fixedPoint, w, signed)))
              println("Stored Result: %f".format(fixedToFloat(dut.io.debugCycleInputs.get(2).peek().litValue, fixedPoint * 2, wResult, signed)))
              println()
            }

            println("DEBUG RESULTS - Fixed")
            for (i <- 0 until numberOfRows) {
              for (j <- 0 until numberOfColumns) {
                print(dut.io.debugResults.get(i)(j).peek().litValue)
                print(" ")
              }
              println()
            }
            println()
            println("DEBUG RESULTS - Float")
            val resultFixed: Array[Array[BigInt]] = Array.fill(multiplicationResultFixed.length, multiplicationResultFixed(0).length)(0)
            for (i <- multiplicationResultFixed.indices) {
              for (j <- multiplicationResultFixed(0).indices) {
                resultFixed(i)(j) = dut.io.outputChannel.bits(i)(j).peek().litValue
              }
            }
            val resultFloat = convertFixedMatrixToFloatMatrix(resultFixed, fixedPoint * 2, wResult, signed)
            print(matrixToString(resultFloat))
            println()
          }
          cycles = cycles + 1
          dut.clock.step()
        }

        if (printInterfaces) {
          println("Interfaces at end:")
          println("Input Channel Valid: %b".format(dut.io.inputChannel.valid.peek().litToBoolean))
          println("Input Channel Ready: %b".format(dut.io.inputChannel.ready.peek().litToBoolean))
          println("Weight Channel Valid: %b".format(dut.io.weightChannel.valid.peek().litToBoolean))
          println("Weight Channel Ready: %b".format(dut.io.weightChannel.ready.peek().litToBoolean))
          println("Result Channel Valid: %b".format(dut.io.outputChannel.valid.peek().litToBoolean))
          println("Result Channel Ready: %b".format(dut.io.outputChannel.ready.peek().litToBoolean))
          println("DEBUG COMPUTATION START: %b".format(dut.io.debugComputationStart.get.peek().litToBoolean))
          println()
        }

        val resultFixed: Array[Array[BigInt]] = Array.fill(multiplicationResultFixed.length, multiplicationResultFixed(0).length)(0)
        for (i <- multiplicationResultFixed.indices) {
          for (j <- multiplicationResultFixed(0).indices) {
            resultFixed(i)(j) = dut.io.outputChannel.bits(i)(j).peek().litValue
          }
        }

        val resultFloat = convertFixedMatrixToFloatMatrix(resultFixed, fixedPoint * 2, wResult, signed)
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