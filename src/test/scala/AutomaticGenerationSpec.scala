
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import scala_utils.FixedPointConversion.{fixedToFloat, floatToFixed}

class AutomaticGenerationSpec extends AnyFreeSpec with ChiselScalatestTester {

  val printToFile = false // set to true to print the results to a file
  val printToConsole = true // set to true to print the results to the console
  val printConnections = false // set to true to print the connections to the console
  //val filepath = "src/main/scala/scala_utils/data/example_spec_file.json"
  val filepath = "ONNX Python/example_spec_file.json"

  val w = 10
  val wResult = 4 * w
  val fixedPoint = 6
  val fixedPointResult = 2 * fixedPoint
  val signed = true
  val threshold = 0.25f
  val numberOfInputs = 10
  val pipelineIO = true

  val inputs = (0 until numberOfInputs).map(i => 2 * Math.PI * i / numberOfInputs.toDouble)
  val inputsFixed = inputs.map(i => floatToFixed(i.toFloat, fixedPointResult, wResult, signed))
  val results = Array.fill(numberOfInputs)(0.0f)
  val expected = inputs.map(i => Math.sin(i))

  val lists: (_, List[Any], List[List[Int]]) = SpecToListConverter.convertSpecToLists(filepath)

  // Print the lists
  if (printToConsole) {
    println(lists._2)
    println()
    println(lists._3)
    println()
  }

  var done = 0 // keep track of how many tests are done to write the results to a file when all tests are done
  for (testNum <- 0 until numberOfInputs) {
    "AutomaticGenerationSpec should behave correctly for test %d (input = %f, expect = %f)".format(testNum, inputs(testNum), expected(testNum)) in {
      test(new AutomaticGeneration(lists._2, lists._3, pipelineIO, true, printConnections)) { dut =>
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
        val resultFixed = dut.io.outputChannel.bits.peek().litValue

        if (printToConsole) {
          println("Test: " + testNum)
          println("Input: " + inputs(testNum))
          println("Output: " + fixedToFloat(resultFixed, fixedPointResult, wResult, signed))
          println("Output Fixed: " + resultFixed)
          println("Expected: " + expected(testNum))
          println("Cycles: " + cycleTotal)
          println()
        }
        results(testNum) = fixedToFloat(resultFixed, fixedPointResult, wResult, signed)

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