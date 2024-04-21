
import chisel3._
import chiseltest._
import onnx.Operators.Parameters
import org.scalatest.freespec.AnyFreeSpec
import scala_utils.FixedPointConversion.{fixedToFloat, floatToFixed}
import TestingUtils.Comparison
import onnx.SpecToListConverter

class AutomaticGenerationUCIMLSpec extends AnyFreeSpec with ChiselScalatestTester {
  val printToFile = false // set to true to print the results to a file
  val printToConsole = true // set to true to print the results to the console
  val printConnections = true // set to true to print the connections to the console
  val filepath = "ONNX Python/json/8x8.json"

  val lists: (Parameters, List[Any], List[List[Int]]) = SpecToListConverter.convertSpecToLists(filepath, true)
  val parameters = lists._1

  val w = parameters.w
  val wResult = parameters.wResult
  val fixedPoint = parameters.fixedPoint
  val fixedPointResult = parameters.fixedPointResult

  val signed = true
  val threshold = 1.75f
  val numberOfInputs = 10

  val imageSize = 8

  // Print the lists
  if (printToConsole) {
    println(lists._1)
    println()
    println(lists._2)
    println()
    println(lists._3)
    println()
  }

  "AutomaticGenerationSpec should behave correctly" in {
    test(new AutomaticGeneration(lists._2, lists._3, printConnections)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut => // test with verilator
      dut.clock.setTimeout(100000)
      for (testNum <- 0 until numberOfInputs) {

        dut.reset.poke(true.B)
        dut.clock.step()
        dut.reset.poke(false.B)
        dut.clock.step()

        println("Test " + testNum + " begun")

        // Read flat data from ONNX Python/input.txt
        val inputFileName = "ONNX Python/digits_8x8/%d.txt".format(testNum)
        println(inputFileName)
        val flatData = scala.io.Source.fromFile(inputFileName).getLines().map(_.toFloat).toArray
        val fixedFlatData = flatData.map(i => floatToFixed(i, fixedPoint, w, signed))

        //Group to 8x8
        val groupedData = Array.fill(imageSize, imageSize)(BigInt(0))
        for (i <- 0 until imageSize) {
          for (j <- 0 until imageSize) {
            groupedData(i)(j) = fixedFlatData(i * imageSize + j)
          }
        }

        val expectedFileName = "ONNX Python/digits_8x8/expected_%d.txt".format(testNum)
        val expectedFlatOutput = scala.io.Source.fromFile(expectedFileName).getLines().map(_.toFloat).toArray

        var cycleTotal = 0
        for (i <- 0 until imageSize) {
          for (j <- 0 until imageSize) {
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

        dut.clock.step()
        dut.io.inputChannel.valid.poke(false.B)
        dut.io.outputChannel.ready.poke(false.B)
        dut.clock.step()

        if (printToConsole) {
          println("Test: " + testNum)
          println("Output:  \t\t" + resultsFloat.map(f => "%+3.2f".format(f)).mkString("\t"))
          println("Expected:\t\t" + expectedFlatOutput.map(f => "%+3.2f".format(f)).mkString("\t"))
          println("Cycles: " + cycleTotal)
          println()
        }

        for (i <- 0 until 10) {
          assert(
            Comparison.CompareWithErrorThreshold(resultsFloat(i), expectedFlatOutput(i), threshold),
            ": (test %d) did not match (got %f : expected %f)".format(testNum, resultsFloat(i), expectedFlatOutput(i))
          )
        }
      }
    }
  }
}
