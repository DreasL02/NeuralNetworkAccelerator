
import chisel3._
import chiseltest._
import onnx.Operators.Parameters
import org.scalatest.freespec.AnyFreeSpec
import scala_utils.FileReader.readMatrixFromFile
import scala_utils.FixedPointConversion.{convertFloatMatrixToFixedMatrix, fixedToFloat, floatToFixed}

class SineNetworkPipeSpec extends AnyFreeSpec with ChiselScalatestTester {

  val printToConsole = true // set to true to print the results to the console

  // Load the weights and biases into the ROMs from the files stored in the scala_utils/data folder
  val weightsL1 = readMatrixFromFile("src/main/scala/scala_utils/data/matlab-inference-data_w1.txt")
  val biasesL1 = readMatrixFromFile("src/main/scala/scala_utils/data/matlab-inference-data_b1.txt")
  val weightsL2 = readMatrixFromFile("src/main/scala/scala_utils/data/matlab-inference-data_w2.txt")
  val biasesL2 = readMatrixFromFile("src/main/scala/scala_utils/data/matlab-inference-data_b2.txt")
  val weightsL3 = readMatrixFromFile("src/main/scala/scala_utils/data/matlab-inference-data_w3.txt")
  val biasesL3 = readMatrixFromFile("src/main/scala/scala_utils/data/matlab-inference-data_b3.txt")

  val w = 8
  val wResult = 2 * w
  val fixedPoint = 4
  val fixedPointResult = fixedPoint * 2
  val signed = true
  val threshold = 0.25f
  val numberOfInputs = 10

  // convert to fixed point using the same fixed point and sign for all layers
  val weightsL1Fixed = convertFloatMatrixToFixedMatrix(weightsL1, fixedPoint, w, signed)
  val biasesL1Fixed = convertFloatMatrixToFixedMatrix(biasesL1, fixedPoint * 2, wResult, signed)
  val weightsL2Fixed = convertFloatMatrixToFixedMatrix(weightsL2, fixedPoint, w, signed)
  val biasesL2Fixed = convertFloatMatrixToFixedMatrix(biasesL2, fixedPoint * 2, wResult, signed)
  val weightsL3Fixed = convertFloatMatrixToFixedMatrix(weightsL3, fixedPoint, w, signed)
  val biasesL3Fixed = convertFloatMatrixToFixedMatrix(biasesL3, fixedPoint * 2, wResult, signed)

  // collect the weights and biases into arrays
  val weights = Array(weightsL1Fixed, weightsL2Fixed, weightsL3Fixed)
  val biases = Array(biasesL1Fixed, biasesL2Fixed, biasesL3Fixed)

  val inputs = (0 until numberOfInputs).map(i => 2 * Math.PI * i / numberOfInputs.toDouble)
  val inputsFixed = inputs.map(i => floatToFixed(i.toFloat, fixedPoint, w, signed))
  val results = Array.fill(numberOfInputs)(0.0f)
  val expected = inputs.map(i => Math.sin(i))

  var done = 0 // keep track of how many tests are done to write the results to a file when all tests are done
  "Should work" in {
    test(new SineNetwork(w, wResult, signed, fixedPoint, weights, biases, true)) { dut =>
      var inputNum = 0
      var resultNum = 0
      var cycleTotal = 0

      while (resultNum < numberOfInputs) {
        dut.io.outputChannel.ready.poke(true.B)

        if (printToConsole) {
          println()
          println("Result: " + fixedToFloat(dut.io.outputChannel.bits(0)(0).peek().litValue, fixedPointResult, wResult, signed) + " Expected: " + expected(resultNum) + " Cycles: " + cycleTotal)
          println("General ready: " + dut.io.inputChannel.ready.peek().litToBoolean + " Valid: " + dut.io.outputChannel.valid.peek().litToBoolean)
          println("Probe1 ready: " + dut.io.probe1.get.ready.peek().litToBoolean + " Valid: " + dut.io.probe1.get.valid.peek().litToBoolean + " Value: " + fixedToFloat(dut.io.probe1.get.value.peek().litValue, fixedPointResult, wResult, signed))
          println("Probe2 ready: " + dut.io.probe2.get.ready.peek().litToBoolean + " Valid: " + dut.io.probe2.get.valid.peek().litToBoolean + " Value: " + fixedToFloat(dut.io.probe2.get.value.peek().litValue, fixedPointResult, wResult, signed))
          println("Probe3 ready: " + dut.io.probe3.get.ready.peek().litToBoolean + " Valid: " + dut.io.probe3.get.valid.peek().litToBoolean + " Value: " + fixedToFloat(dut.io.probe3.get.value.peek().litValue, fixedPointResult, wResult, signed))
          println("Probe4 ready: " + dut.io.probe4.get.ready.peek().litToBoolean + " Valid: " + dut.io.probe4.get.valid.peek().litToBoolean + " Value: " + fixedToFloat(dut.io.probe4.get.value.peek().litValue, fixedPointResult, wResult, signed))
        }
        if (inputNum >= numberOfInputs) {
          dut.io.inputChannel.valid.poke(false.B)
        } else {
          dut.io.inputChannel.valid.poke(true.B)
        }

        if (dut.io.inputChannel.ready.peek().litToBoolean && inputNum < numberOfInputs) {
          if (printToConsole) println("=== Inputted: " + fixedToFloat(inputsFixed(inputNum), fixedPoint, w, signed) + " Cycles: " + cycleTotal + " ===")
          dut.io.inputChannel.bits(0)(0).poke(inputsFixed(inputNum).U)
          inputNum += 1
        }

        if (dut.io.outputChannel.valid.peek().litToBoolean) {
          val resultFixed = dut.io.outputChannel.bits(0)(0).peek().litValue
          results(resultNum) = fixedToFloat(resultFixed, fixedPointResult, wResult, signed)
          if (printToConsole) println("=== Result: " + results(resultNum) + " Expected: " + expected(resultNum) + " Cycles: " + cycleTotal + " ===")
          resultNum += 1
        }


        dut.clock.step()
        cycleTotal += 1

        if (cycleTotal > 200 * numberOfInputs) {
          fail("Timeout")
        }
      }
    }
  }
}