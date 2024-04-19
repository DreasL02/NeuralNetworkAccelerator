
import chisel3._
import chiseltest._
import onnx.Operators.Parameters
import org.scalatest.freespec.AnyFreeSpec
import scala_utils.FixedPointConversion.{fixedToFloat, floatToFixed}
import TestingUtils.Comparison

class AutomaticGenerationUCIMLPipeSpec extends AnyFreeSpec with ChiselScalatestTester {
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
  val pipelineIO = false

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


  val testData = Array.fill(numberOfInputs)(Array.ofDim[Float](imageSize, imageSize))
  for (testNum <- 0 until numberOfInputs) {
    val inputFileName = "ONNX Python/digits_8x8/%d.txt".format(testNum)
    println(inputFileName)
    val flatData = scala.io.Source.fromFile(inputFileName).getLines().map(_.toFloat).toArray
    for (i <- 0 until imageSize) {
      for (j <- 0 until imageSize) {
        testData(testNum)(i)(j) = flatData(i * imageSize + j)
      }
    }
  }

  val expectedResults = Array.fill(numberOfInputs)(Array.ofDim[Float](10))
  for (testNum <- 0 until numberOfInputs) {
    val inputFileName = "ONNX Python/digits_8x8/expected_%d.txt".format(testNum)
    println(inputFileName)
    val flatData = scala.io.Source.fromFile(inputFileName).getLines().map(_.toFloat).toArray
    for (i <- 0 until 10) {
      expectedResults(testNum)(i) = flatData(i)
    }
  }

  val results = Array.fill(numberOfInputs)(Array.ofDim[Float](10))


  "AutomaticGenerationSpec should behave correctly" in {
    //test(new AutomaticGeneration(lists._2, lists._3, pipelineIO, true, printConnections)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut => // test with verilator
    test(new AutomaticGeneration(lists._2, lists._3, pipelineIO, true, printConnections)) { dut => // test with verilator
      dut.clock.setTimeout(100000)

      var inputNum = 0
      var resultNum = 0
      var cycleTotal = 0

      while (resultNum < numberOfInputs) {
        dut.io.inputChannel.valid.poke(true.B)
        dut.io.outputChannel.ready.poke(true.B)

        if (dut.io.inputChannel.ready.peek().litToBoolean && inputNum < numberOfInputs) {
          if (printToConsole) {
            println("Inputted image " + inputNum + " Cycles: " + cycleTotal)
            println()
          }
          for (i <- 0 until imageSize) {
            for (j <- 0 until imageSize) {
              dut.io.inputChannel.bits(0)(0)(i)(j).poke(floatToFixed(testData(inputNum)(i)(j), fixedPoint, w, signed).U)
            }
          }
          inputNum += 1
        }

        if (dut.io.outputChannel.valid.peek().litToBoolean) {
          val resultFixed = Array.fill(10)(BigInt(0))
          for (i <- 0 until 10) {
            resultFixed(i) = dut.io.outputChannel.bits(0)(0)(0)(i).peek().litValue
            results(resultNum)(i) = fixedToFloat(resultFixed(i), fixedPointResult, wResult, signed)
          }
          if (printToConsole) {
            println("Result for image " + resultNum + " Cycles Total: " + cycleTotal)
            println("Output:  \t\t" + results(resultNum).map(f => "%+3.2f".format(f)).mkString("\t"))
            println("Expected:\t\t" + expectedResults(resultNum).map(f => "%+3.2f".format(f)).mkString("\t"))
            println()
          }
          resultNum += 1
        }

        dut.clock.step()
        cycleTotal += 1

        if (cycleTotal > 100000) {
          fail("Timeout")
        }
      }

      // Assert that the results are within the threshold
      for (testNum <- 0 until numberOfInputs) {
        for (i <- 0 until 10) {
          assert(
            Comparison.CompareWithErrorThreshold(results(testNum)(i), expectedResults(testNum)(i), threshold),
            ": (test %d) did not match (got %f : expected %f)".format(testNum, results(testNum)(i), expectedResults(testNum)(i))
          )
        }
      }
    }
  }
}
