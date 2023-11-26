import Utils.MatrixUtils._
import Utils.FixedPointConverter._
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class LayerCalculatorSpec extends AnyFreeSpec with ChiselScalatestTester {
  val enablePrintingInFirstTest = true
  "LayerCalculator should behave correctly when given a set of values (3x3 matrices, fixed point at 3)" in {
    val dimension = 4 //=3
    test(new LayerCalculator(w = 8, dimension = dimension)) { dut =>
      /*
      var inputsFloat = Array(Array(1.2f, 1.3f, 2.4f), Array(0.9f, 3.4f, 0.9f), Array(2.2f, 1.2f, 0.9f))
      var weightsFloat = Array(Array(2.2f, 1.3f, 1.0f), Array(4.9f, 0.4f, 4.8f), Array(2.2f, 1.2f, 0.9f))
      var biasesFloat = Array(Array(1.0f, 1.0f, 1.0f), Array(1.0f, 1.0f, 1.0f), Array(1.0f, 1.0f, 1.0f))*/
      var inputsFloat = Array(Array(1.2f, 1.3f, 2.4f, 1.0f), Array(0.9f, 3.4f, 0.9f, 2.2f), Array(2.2f, 1.2f, 0.9f, 1.2f), Array(2.2f, 1.2f, 0.9f, 1.2f))
      var weightsFloat = Array(Array(2.2f, 1.3f, 1.0f, 0.2f), Array(4.9f, 0.4f, 4.8f, 4.2f), Array(2.2f, 1.2f, 0.9f, 1.2f), Array(2.2f, 1.2f, 0.9f, 1.2f))
      var biasesFloat = Array(Array(1.0f, 1.0f, 1.0f, 1.0f), Array(1.0f, 1.0f, 1.0f, 1.0f), Array(1.0f, 1.0f, 1.0f, 1.0f), Array(1.0f, 1.0f, 1.0f, 1.0f))

      val fixedPoint = 2
      val signed = 0

      var multiplicationResultFloat = calculateMatrixMultiplication(inputsFloat, weightsFloat)
      var additionResultFloat = calculateMatrixAddition(multiplicationResultFloat, biasesFloat)

      if (enablePrintingInFirstTest)
        printMatrixMAC(inputsFloat, weightsFloat, biasesFloat, additionResultFloat, "GOLDEN MODEL CALCULATION IN PURE FLOATING")

      val inputsFixed = convertFloatMatrixToFixedMatrix(inputsFloat, fixedPoint)
      val weightsFixed = convertFloatMatrixToFixedMatrix(weightsFloat, fixedPoint)
      val biasesFixed = convertFloatMatrixToFixedMatrix(biasesFloat, fixedPoint)

      val multiplicationResultFixed = calculateMatrixMultiplication(inputsFixed, weightsFixed)
      val additionResultFixed = calculateMatrixAddition(multiplicationResultFixed, biasesFixed)

      if (enablePrintingInFirstTest)
        printMatrixMAC(inputsFixed, weightsFixed, biasesFixed, additionResultFixed, "GOLDEN MODEL CALCULATION IN FIXED POINT")

      inputsFloat = convertFixedMatrixToFloatMatrix(inputsFixed, fixedPoint)
      weightsFloat = convertFixedMatrixToFloatMatrix(weightsFixed, fixedPoint)
      biasesFloat = convertFixedMatrixToFloatMatrix(biasesFixed, fixedPoint)

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

      var cycles = 0
      while (!dut.io.valid.peekBoolean()) {
        dut.clock.step()
        cycles = cycles + 1
        dut.io.load.poke(false.B)
        dut.io.fixedPoint.poke(2.U) // should be ignored when load is false
        dut.io.signed.poke(1.U) // should be ignored when load is false
      }


      val resultFixed = Array.fill(additionResultFixed.length, additionResultFixed(0).length)(0)
      for (i <- additionResultFixed.indices) {
        for (j <- additionResultFixed(0).indices) {
          resultFixed(i)(j) = dut.io.result(i)(j).peek().litValue.toInt
        }
      }

      val resultFloat = convertFixedMatrixToFloatMatrix(resultFixed, fixedPoint)
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