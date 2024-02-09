
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import utils.MatrixUtils._
import utils.FixedPointConversion._

class LayerCalculatorSpec extends AnyFreeSpec with ChiselScalatestTester {
  val dimension = 3 //4 //=3
  val w = 8
  val wStore = 4 * w
  val fixedPoint = 3
  val signed = 1
  val enablePrintingInFirstTest = true
  "LayerCalculator should behave correctly when given a set of values (3x3 matrices, fixed point at 3)" in {
    test(new LayerCalculator(w = w, wStore = wStore, xDimension = dimension, yDimension = dimension, enableDebuggingIO = true)) { dut =>
      var inputsFloat = Array(Array(1.2f, 1.3f, 2.4f), Array(0.9f, 3.4f, 0.9f), Array(2.2f, 1.2f, 0.9f))
      var weightsFloat = Array(Array(2.2f, 1.3f, 1.0f), Array(4.9f, 0.4f, 4.8f), Array(2.2f, 1.2f, 0.9f))
      var biasesFloat = Array(Array(-2.0f, -2.0f, -1.0f), Array(0.0f, 2.0f, 2.0f), Array(2.0f, 2.0f, 2.0f))

      var multiplicationResultFloat = calculateMatrixMultiplication(inputsFloat, weightsFloat)
      var additionResultFloat = calculateMatrixAddition(multiplicationResultFloat, biasesFloat)

      if (enablePrintingInFirstTest)
        printMatrixMAC(inputsFloat, weightsFloat, biasesFloat, additionResultFloat, "GOLDEN MODEL CALCULATION IN PURE FLOATING")

      val inputsFixed = convertFloatMatrixToFixedMatrix(inputsFloat, fixedPoint, w, signed == 1)
      val weightsFixed = convertFloatMatrixToFixedMatrix(weightsFloat, fixedPoint, w, signed == 1)
      val biasesFixed = convertFloatMatrixToFixedMatrix(biasesFloat, fixedPoint * 2, wStore, signed == 1)
      val biasesFixedForTesting = convertFloatMatrixToFixedMatrix(biasesFloat, fixedPoint, w, signed == 1)

      println("Biases fixed")
      for (i <- 0 until dimension) {
        for (j <- 0 until dimension) {
          print(biasesFixed(i)(j))
          print(" ")
        }
        println()
      }

      val multiplicationResultFixed = calculateMatrixMultiplication(inputsFixed, weightsFixed)
      val additionResultFixed = calculateMatrixAddition(multiplicationResultFixed, biasesFixedForTesting)

      if (enablePrintingInFirstTest)
        printMatrixMAC(inputsFixed, weightsFixed, biasesFixedForTesting, additionResultFixed, "GOLDEN MODEL CALCULATION IN FIXED POINT")

      inputsFloat = convertFixedMatrixToFloatMatrix(inputsFixed, fixedPoint, w, signed == 1)
      weightsFloat = convertFixedMatrixToFloatMatrix(weightsFixed, fixedPoint, w, signed == 1)
      biasesFloat = convertFixedMatrixToFloatMatrix(biasesFixedForTesting, fixedPoint, w, signed == 1)

      multiplicationResultFloat = calculateMatrixMultiplication(inputsFloat, weightsFloat)
      additionResultFloat = calculateMatrixAddition(multiplicationResultFloat, biasesFloat)

      if (enablePrintingInFirstTest)
        printMatrixMAC(inputsFloat, weightsFloat, biasesFloat, additionResultFloat, "GOLDEN MODEL CALCULATION IN AFTER TRANSFORMATION BACK TO FLOATING")

      // Setup the dut
      dut.io.load.poke(true.B)
      dut.io.signed.poke(signed.asUInt)
      dut.io.fixedPoint.poke(fixedPoint)
      for (i <- inputsFixed.indices) {
        for (j <- inputsFixed(0).indices) {
          dut.io.inputs(i)(j).poke(inputsFixed(i)(dimension - 1 - j).asUInt) // correct format, reverse order in y
          dut.io.weights(i)(j).poke(weightsFixed(dimension - 1 - j)(i).asUInt) // correct format, reverse order in x
          dut.io.biases(i)(j).poke(biasesFixed(i)(j).asUInt)
        }
      }

      // All values should now be loaded
      dut.clock.step()
      dut.io.load.poke(false.B)
      dut.io.fixedPoint.poke(2.U) // should be ignored when load is false
      dut.io.signed.poke(1.U) // should be ignored when load is false

      var cycles = 0
      while (!dut.io.valid.peekBoolean()) {
        // print all debugBiases values
        println("Cycle %d".format(cycles))
        if (enablePrintingInFirstTest) {
          println("DEBUG SYSTOLIC ARRAY RESULTS")
          for (i <- 0 until dimension) {
            for (j <- 0 until dimension) {
              print(dut.io.debugSystolicArrayResults.get(i)(j).peek().litValue)
              print(" ")
            }
            println()
          }
          println("DEBUG BIASES")
          for (i <- 0 until dimension) {
            for (j <- 0 until dimension) {
              print(dut.io.debugBiases.get(i)(j).peek().litValue)
              print(" ")
            }
            println()
          }
          println("ROUNDER INPUTS")
          for (i <- 0 until dimension) {
            for (j <- 0 until dimension) {
              print(dut.io.debugRounderInputs.get(i)(j).peek().litValue)
              print(" ")
            }
            println()
          }
          println("ReLU INPUTS")
          for (i <- 0 until dimension) {
            for (j <- 0 until dimension) {
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

      val resultFloat = convertFixedMatrixToFloatMatrix(resultFixed, fixedPoint, w, signed == 1)
      if (enablePrintingInFirstTest) {
        println("DONE IN %d CYCLES".format(cycles))
        println("RESULT IN FLOATING POINT")
        print(matrixToString(resultFloat))
        println("RESULT IN FIXED POINT")
        print(matrixToString(resultFixed))
      }

      // Evaluate
      for (i <- additionResultFloat.indices) {
        for (j <- additionResultFloat(0).indices) {
          val a = resultFloat(i)(j)
          val b = additionResultFloat(i)(j)
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