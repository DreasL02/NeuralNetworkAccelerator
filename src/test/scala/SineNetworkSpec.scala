import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import scala_utils.FileReader._
import scala_utils.FixedPointConversion._

class SineNetworkSpec extends AnyFreeSpec with ChiselScalatestTester {
  // Load the weights and biases into the ROMs from the files stored in the scala_utils/data folder
  val weightsL1 = readMatrixFromFile("src/main/scala/scala_utils/data/matlab-inference-data_w1.txt")
  val biasesL1 = readMatrixFromFile("src/main/scala/scala_utils/data/matlab-inference-data_b1.txt")
  val weightsL2 = readMatrixFromFile("src/main/scala/scala_utils/data/matlab-inference-data_w2.txt")
  val biasesL2 = readMatrixFromFile("src/main/scala/scala_utils/data/matlab-inference-data_b2.txt")
  val weightsL3 = readMatrixFromFile("src/main/scala/scala_utils/data/matlab-inference-data_w3.txt")
  val biasesL3 = readMatrixFromFile("src/main/scala/scala_utils/data/matlab-inference-data_b3.txt")

  val w = 8
  val wResult = 32
  val fixedPoint = 0
  val signed = true

  // convert to fixed point using the same fixed point and sign for all layers
  val weightsL1Fixed = convertFloatMatrixToFixedMatrix(weightsL1, fixedPoint, w, signed)
  val biasesL1Fixed = convertFloatMatrixToFixedMatrix(biasesL1, fixedPoint, wResult, signed)
  val weightsL2Fixed = convertFloatMatrixToFixedMatrix(weightsL2, fixedPoint, w, signed)
  val biasesL2Fixed = convertFloatMatrixToFixedMatrix(biasesL2, fixedPoint, wResult, signed)
  val weightsL3Fixed = convertFloatMatrixToFixedMatrix(weightsL3, fixedPoint, w, signed)
  val biasesL3Fixed = convertFloatMatrixToFixedMatrix(biasesL3, fixedPoint, wResult, signed)

  // collect the weights and biases into arrays
  val weights = Array(weightsL1Fixed, weightsL2Fixed, weightsL3Fixed)
  val biases = Array(biasesL1Fixed, biasesL2Fixed, biasesL3Fixed)

  val input = 1.0f
  val inputFixed = floatToFixed(input, fixedPoint, w, signed)

  "SineNetwork should behave correctly" in {
    test(new SineNetwork(w, wResult, signed, fixedPoint, weights, biases, true)) { dut =>
      dut.io.loads(0).poke(true.B)
      dut.io.loads(1).poke(false.B)
      dut.io.loads(2).poke(false.B)

      dut.io.input.poke(inputFixed.U)

      dut.clock.step()
      dut.io.loads(0).poke(false.B)
      while (!dut.io.valids(0).peekBoolean()) {
        dut.clock.step()
      }

      println("MMU1 Result")
      for (i <- 0 until dut.io.debugMMU1Result.get.length) {
        for (j <- 0 until dut.io.debugMMU1Result.get(i).length) {
          print(dut.io.debugMMU1Result.get(i)(j).peek().litValue)
          print(" ")
        }
        println()
      }

      dut.io.loads(1).poke(true.B)
      dut.clock.step()
      dut.io.loads(1).poke(false.B)
      while (!dut.io.valids(1).peekBoolean()) {
        dut.clock.step()
      }

      println("MMU2 Result")
      for (i <- 0 until dut.io.debugMMU2Result.get.length) {
        for (j <- 0 until dut.io.debugMMU2Result.get(i).length) {
          print(dut.io.debugMMU2Result.get(i)(j).peek().litValue)
          print(" ")
        }
        println()
      }


      dut.io.loads(2).poke(true.B)
      dut.clock.step()
      dut.io.loads(2).poke(false.B)
      while (!dut.io.valids(2).peekBoolean()) {
        dut.clock.step()
      }

      println("MMU3 Result")
      for (i <- 0 until dut.io.debugMMU3Result.get.length) {
        for (j <- 0 until dut.io.debugMMU3Result.get(i).length) {
          print(dut.io.debugMMU3Result.get(i)(j).peek().litValue)
          print(" ")
        }
        println()
      }

      val resultFixed = dut.io.output.peek().litValue
      println("Output: " + fixedToFloat(resultFixed, fixedPoint, w, signed))
      println("Expected: " + Math.sin(input))
    }
  }
}
