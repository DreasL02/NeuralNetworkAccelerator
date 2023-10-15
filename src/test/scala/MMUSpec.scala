import Utils.MatrixUtils._
import Utils.FixedPointConverter._
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class MMUSpec extends AnyFreeSpec with ChiselScalatestTester {
  "Matrix Multiplication Unit should behave correctly" in {
    val dimension = 3
    test(new MatrixMultiplicationUnit(w = 16, dimension = dimension)) { dut =>
      var inputsFloat = Array(Array(1.0f, 2.3f, 3.0f), Array(4.0f, 5.05f, 6.0f), Array(7.0f, 8.6f, 9.0f))
      var weightsFloat = Array(Array(1.0f, 2.0f, 3.0f), Array(4.8f, 5.0f, 6.0f), Array(7.0f, 8.0f, 9.0f))
      var biasesFloat = Array(Array(1.0f, 2.0f, 3.4f), Array(4.0f, 5.0f, 6.0f), Array(7.0f, 8.0f, 9.0f))
      val fixedPoint = 6
      val signed = 0

      var multiplicationResultFloat = calculateMatrixMultiplication(inputsFloat, weightsFloat)
      var additionResultFloat = calculateMatrixAddition(multiplicationResultFloat, biasesFloat)

      printMatrixMAC(inputsFloat, weightsFloat, biasesFloat, additionResultFloat, "GOLDEN MODEL CALCULATION IN PURE FLOATING")

      val inputsFixed = convertFloatMatrixToFixedMatrix(inputsFloat, fixedPoint)
      val weightsFixed = convertFloatMatrixToFixedMatrix(weightsFloat, fixedPoint)
      val biasesFixed = convertFloatMatrixToFixedMatrix(biasesFloat, fixedPoint)

      val multiplicationResultFixed = calculateMatrixMultiplication(inputsFixed, weightsFixed)
      val additionResultFixed = calculateMatrixAddition(multiplicationResultFixed, biasesFixed)

      printMatrixMAC(inputsFixed, weightsFixed, biasesFixed, additionResultFixed, "GOLDEN MODEL CALCULATION IN FIXED POINT")

      inputsFloat = convertFixedMatrixToFloatMatrix(inputsFixed, fixedPoint)
      weightsFloat = convertFixedMatrixToFloatMatrix(weightsFixed, fixedPoint)
      biasesFloat = convertFixedMatrixToFloatMatrix(biasesFixed, fixedPoint)

      multiplicationResultFloat = calculateMatrixMultiplication(inputsFloat, weightsFloat)
      additionResultFloat = calculateMatrixAddition(multiplicationResultFloat, biasesFloat)

      printMatrixMAC(inputsFloat, weightsFloat, biasesFloat, additionResultFloat, "GOLDEN MODEL CALCULATION IN AFTER TRANSFORMATION BACK TO FLOATING")

      dut.io.signed.poke(signed.asUInt)
      dut.io.fixedPoint.poke(fixedPoint)
      dut.io.loadInputs.poke(true.B)
      dut.io.loadWeights.poke(true.B)
      dut.io.loadBiases.poke(true.B)
      //Assignment of values to buffers missing

      for (i <- inputsFixed.indices) {
        for (j <- inputsFixed(0).indices) {
          dut.io.inputs(i)(j).poke(inputsFixed(i)(dimension - 1 - j).asUInt)
          dut.io.weights(i)(j).poke(weightsFixed(dimension - 1 - j)(i).asUInt)
          dut.io.biases(i)(j).poke(biasesFixed(i)(j).asUInt)
        }
      }

      var cycles = 0
      while (!dut.io.valid.peekBoolean()) {
        dut.clock.step()
        cycles = cycles + 1
        dut.io.loadInputs.poke(false.B)
        dut.io.loadWeights.poke(false.B)
      }


      val resultFixed = Array.fill(additionResultFixed.length, additionResultFixed(0).length)(0)
      for (i <- additionResultFixed.indices) {
        for (j <- additionResultFixed(0).indices) {
          resultFixed(i)(j) = dut.io.result(i)(j).peek().litValue.toInt
        }
      }

      val resultFloat = convertFixedMatrixToFloatMatrix(resultFixed, fixedPoint)
      println("---- SHOULD MATCH ABOVE ----")
      print(matrixToString(resultFloat))
      println("---- FIXED POINT OF ABOVE ----")
      print(matrixToString(resultFixed))

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