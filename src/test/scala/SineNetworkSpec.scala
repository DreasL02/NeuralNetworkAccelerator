import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import scala_utils.FileReader._
import scala_utils.FixedPointConversion._

class SineNetworkSpec extends AnyFreeSpec with ChiselScalatestTester {

  val printToFile = false // set to true to print the results to a file
  val printToConsole = true // set to true to print the results to the console

  // Load the weights and biases into the ROMs from the files stored in the scala_utils/data folder
  val weightsL1 = readMatrixFromFile("src/main/scala/scala_utils/data/matlab-inference-data_w1.txt")
  val biasesL1 = readMatrixFromFile("src/main/scala/scala_utils/data/matlab-inference-data_b1.txt")
  val weightsL2 = readMatrixFromFile("src/main/scala/scala_utils/data/matlab-inference-data_w2.txt")
  val biasesL2 = readMatrixFromFile("src/main/scala/scala_utils/data/matlab-inference-data_b2.txt")
  val weightsL3 = readMatrixFromFile("src/main/scala/scala_utils/data/matlab-inference-data_w3.txt")
  val biasesL3 = readMatrixFromFile("src/main/scala/scala_utils/data/matlab-inference-data_b3.txt")

  val w = 16
  val wResult = 4 * w
  val fixedPoint = 10
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
  for (testNum <- 0 until numberOfInputs) {
    "SineNetwork should behave correctly for test %d (input = %f, expect = %f)".format(testNum, inputs(testNum), expected(testNum)) in {
      test(new SineNetwork(w, wResult, signed, fixedPoint, weights, biases, true)) { dut =>
        var cycleTotal = 0
        dut.io.inputChannel.bits(0)(0).poke(inputsFixed(testNum).U)
        dut.io.inputChannel.valid.poke(true.B)
        dut.io.outputChannel.ready.poke(true.B)

        dut.clock.step()
        cycleTotal += 1
        while (!dut.io.outputChannel.valid.peek().litToBoolean) {
          dut.clock.step()
          cycleTotal += 1

          if (cycleTotal > 1000) {
            fail("Timeout")
          }
        }
        val resultFixed = dut.io.outputChannel.bits(0)(0).peek().litValue

        if (printToConsole) {
          println("Input: " + inputs(testNum))
          println("Output: " + fixedToFloat(resultFixed, fixedPoint, w, signed))
          println("Expected: " + expected(testNum))
          println("Cycles: " + cycleTotal)
        }
        results(testNum) = fixedToFloat(resultFixed, fixedPoint, w, signed)

        // Evaluate
        val a = results(testNum)
        val b = expected(testNum).toFloat
        var valid = false
        if (a - threshold <= b && a + threshold >= b) { //within +-threshold of golden model
          valid = true
        }
        assert(valid, ": input %f (test %d) did not match (got %f : expected %f)".format(inputs(testNum), testNum, a, b))

        done += 1
        if (done == numberOfInputs && printToFile) {
          val file = new java.io.PrintWriter("src/main/scala/scala_utils/data/sine_results.txt")
          // replace all '.' with ',' to make it easier to import into danish excel
          file.write(results.mkString("; ").replace('.', ','))
          file.close()

          val file2 = new java.io.PrintWriter("src/main/scala/scala_utils/data/sine_expected.txt")
          // replace all '.' with ',' to make it easier to import into danish excel
          file2.write(expected.mkString("; ").replace('.', ','))
          file2.close()

          val file3 = new java.io.PrintWriter("src/main/scala/scala_utils/data/sine_inputs.txt")
          // replace all '.' with ',' to make it easier to import into danish excel
          file3.write(inputs.mkString("; ").replace('.', ','))
          file3.close()
        }
      }
    }
  }


}
