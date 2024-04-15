
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import scala_utils.FixedPointConversion.{fixedToFloat, floatToFixed}
import onnx.Operators.Parameters

class AutomaticGenerationMNISTSpec extends AnyFreeSpec with ChiselScalatestTester {
  val printToFile = false // set to true to print the results to a file
  val printToConsole = true // set to true to print the results to the console
  val printConnections = true // set to true to print the connections to the console
  val filepath = "ONNX Python/json/smaller_mnist.json"

  val lists: (Parameters, List[Any], List[List[Int]]) = SpecToListConverter.convertSpecToLists(filepath, true)
  val parameters = lists._1

  val w = parameters.w
  val wResult = parameters.wResult
  val fixedPoint = parameters.fixedPoint
  val fixedPointResult = parameters.fixedPointResult

  val signed = true
  val threshold = 0.25f
  val numberOfInputs = 1
  val pipelineIO = false

  // Print the lists
  if (printToConsole) {
    println(lists._1)
    println()
    println(lists._2)
    println()
    println(lists._3)
    println()
  }

  // Read flat data from ONNX Python/input.txt
  val flatData = scala.io.Source.fromFile("ONNX Python/input.txt").getLines().map(_.toFloat).toArray
  val fixedFlatData = flatData.map(i => floatToFixed(i, fixedPoint, w, signed))

  //Group to 28x28
  val groupedData = Array.fill(28, 28)(BigInt(0))
  for (i <- 0 until 28) {
    for (j <- 0 until 28) {
      groupedData(i)(j) = fixedFlatData(i * 28 + j)
    }
  }

  val flatOutput = scala.io.Source.fromFile("ONNX Python/output.txt").getLines().map(_.toFloat).toArray
  val fixedFlatOutput = flatOutput.map(i => floatToFixed(i, fixedPointResult, wResult, signed))

  for (testNum <- 0 until numberOfInputs) {
    "AutomaticGenerationSpec should behave correctly for test %d (input = %f, expect = %f)".format(testNum, testNum.toFloat, testNum.toFloat) in {
      test(new AutomaticGeneration(lists._2, lists._3, pipelineIO, true, printConnections)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut => // test with verilator
        println("Test " + testNum + " begun")
        dut.clock.setTimeout(100000)
        var cycleTotal = 0
        for (i <- 0 until 28) {
          for (j <- 0 until 28) {
            dut.io.inputChannel.bits(0)(0)(i)(j).poke(groupedData(i)(j).U)
          }
        }
        dut.io.inputChannel.valid.poke(true.B)
        dut.io.outputChannel.ready.poke(true.B)
        dut.clock.step()
        cycleTotal += 1
        while (!dut.io.outputChannel.valid.peek().litToBoolean) {
          dut.clock.step()
          cycleTotal += 1
        }

        val resultFixed = Array.fill(10)(BigInt(0))
        val resultsFloat = Array.fill(10)(0.0f)
        for (i <- 0 until 10) {
          resultFixed(i) = dut.io.outputChannel.bits(0)(0)(0)(i).peek().litValue
          resultsFloat(i) = fixedToFloat(resultFixed(i), fixedPointResult, wResult, signed)
        }

        if (printToConsole) {
          println("Test: " + testNum)
          println("Output: " + resultsFloat.mkString(", "))
          println("Output Fixed: " + resultFixed.mkString(", "))
          println("Expected: " + flatOutput.mkString(", "))
          println("Cycles: " + cycleTotal)
          println()
        }

        // Evaluate
        for (i <- 0 until 10) {
          assert(resultsFloat(i) == flatOutput(i))
        }
      }
    }
  }
}