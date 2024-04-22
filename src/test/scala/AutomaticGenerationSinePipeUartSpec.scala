
import TestingUtils.Comparison.CompareWithErrorThreshold
import chisel3._
import chiseltest._
import onnx.Operators.Parameters
import onnx.SpecToListConverter
import org.scalatest.freespec.AnyFreeSpec
import scala_utils.FixedPointConversion.{fixedToFloat, floatToFixed}
import scala_utils.UartCoding

class AutomaticGenerationSinePipeUartSpec extends AnyFreeSpec with ChiselScalatestTester {

  val printToConsole = true // set to true to print the results to the console
  val printConnections = true // set to true to print the connections to the console
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
    test(new AutomaticGeneration(lists._2, lists._3, printConnections)) { dut =>
      var inputNum = 0
      var resultNum = 0
      var cycleTotal = 0

      val xValue = 1.0f
      val xFixed = floatToFixed(xValue, fixedPoint, w, signed)
      val uartBits = UartCoding.encodeByteToUartBits(xFixed.toByte)

      dut.io.inputChannels(0).valid.poke(true.B)
      dut.io.outputChannels(0).ready.poke(true.B)

      for (i <- 0 until uartBits.length) {
        dut.io.inputChannels(0).bits(0)(0)(0)(0).poke(uartBits(i) - '0')
        dut.clock.step(2)
      }

      while (!dut.io.outputChannels(0).valid.peekBoolean()) {
        dut.clock.step()
      }

      println("Output valid")
      val r = dut.io.outputChannels(0).bits(0)(0)(0)(0).peekInt
      val result = fixedToFloat(r, fixedPointResult, wResult, signed)
      println(result)
    }
  }
}