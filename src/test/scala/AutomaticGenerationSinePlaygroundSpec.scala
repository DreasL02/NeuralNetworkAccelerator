
import chisel3._
import chiseltest._
import onnx.Operators.Parameters
import org.scalatest.freespec.AnyFreeSpec
import scala_utils.FixedPointConversion.{fixedToFloat, floatToFixed}
import TestingUtils.Comparison.CompareWithErrorThreshold

class AutomaticGenerationSinePlaygroundSpec extends AnyFreeSpec with ChiselScalatestTester {

  val printToConsole = true // set to true to print the results to the console
  val printConnections = false // set to true to print the connections to the console
  val filepath = "ONNX Python/json/sine.json"

  val lists: (Parameters, List[Any], List[List[Int]]) = SpecToListConverter.convertSpecToLists(filepath)
  val parameters = lists._1

  val w = parameters.w
  val wResult = parameters.wResult
  val fixedPoint = parameters.fixedPoint
  val fixedPointResult = parameters.fixedPointResult
  val signed = true
  val threshold = 0.25f
  val numberOfInputs = 10
  val pipelineIO = false

  val inputs = (0 until numberOfInputs).map(i => 2 * Math.PI * i / numberOfInputs.toDouble)
  val inputsFixed = inputs.map(i => floatToFixed(i.toFloat, fixedPoint, w, signed))
  val results = Array.fill(numberOfInputs)(0.0f)
  val cycleStart = Array.fill(numberOfInputs)(0)
  val expected = inputs.map(i => Math.sin(i).toFloat)

  // Print the lists
  if (printToConsole) {
    println(lists._1)
    println()
    println(lists._2)
    println()
    println(lists._3)
    println()
  }

  "Should work" in {
    test(new AutomaticGeneration(lists._2, lists._3, pipelineIO, true, printConnections)) { dut =>
      var inputNum = 0
      var resultNum = 0
      var cycleTotal = 0

      while (resultNum < numberOfInputs) {
        dut.io.inputChannel.valid.poke(true.B)
        dut.io.outputChannel.ready.poke(true.B)

        if (dut.io.inputChannel.ready.peek().litToBoolean && inputNum < numberOfInputs) {
          println("Inputted: " + fixedToFloat(inputsFixed(inputNum), fixedPoint, w, signed) + " Cycles: " + cycleTotal)
          dut.io.inputChannel.bits(0)(0)(0)(0).poke(inputsFixed(inputNum).U)
          cycleStart(inputNum) = cycleTotal
          inputNum += 1
        }

        if (dut.io.outputChannel.valid.peek().litToBoolean) {
          val resultFixed = dut.io.outputChannel.bits(0)(0)(0)(0).peek().litValue
          results(resultNum) = fixedToFloat(resultFixed, fixedPointResult, wResult, signed)
          println("Result: " + results(resultNum) + " Expected: " + expected(resultNum) + " Cycles Total: " + cycleTotal + " Cycles Since Input: " + (cycleTotal - cycleStart(resultNum)))
          resultNum += 1
        }

        dut.clock.step()
        cycleTotal += 1

        if (cycleTotal > 1000) {
          fail("Timeout")
        }
      }

      // Assert that the results are within the threshold
      for (i <- 0 until numberOfInputs) {
        assert(CompareWithErrorThreshold(results(i), expected(i), threshold), "Entry " + i + " is not within the threshold. Got: " + results(i) + " Expected: " + expected(i))
      }
    }
  }
}