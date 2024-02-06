
import utils.{FixedPointConversion, Mapping}
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class AcceleratorSpec extends AnyFreeSpec with ChiselScalatestTester {
  val w = 8
  val wStore = 4 * w
  val dimension = 3
  val fix = 0
  val sign = 1

  val inputsL1: Array[Array[Float]] = Array(Array(1.2f, 1.3f, 2.4f), Array(0.9f, 3.4f, 0.9f), Array(2.2f, 1.2f, 0.9f))
  val weightsL1: Array[Array[Float]] = Array(Array(-2.2f, 1.3f, 1.0f), Array(4.9f, 0.4f, 4.8f), Array(2.2f, 1.2f, 0.9f))
  val biasesL1: Array[Array[Float]] = Array(Array(-1.0f, -1.0f, -1.0f), Array(-1.0f, -1.0f, -1.0f), Array(-1.0f, -1.0f, -1.0f))
  val signL1: Int = sign
  val fixedPointL1: Int = fix

  val inputsL2: Array[Array[Float]] = Array(Array(0.0f, 0.0f, 0.0f), Array(0.0f, 0.0f, 0.0f), Array(0.0f, 0.0f, 0.0f))
  val weightsL2: Array[Array[Float]] = Array(Array(1.0f, 0.9f, 0.8f), Array(0.7f, 0.6f, 0.4f), Array(0.3f, 0.2f, 0.1f))
  val biasesL2: Array[Array[Float]] = Array(Array(0.0f, 0.0f, 0.0f), Array(1.0f, 1.0f, 1.0f), Array(1.0f, 1.0f, 1.0f))
  val signL2: Int = sign
  val fixedPointL2: Int = fix

  val inputsL3: Array[Array[Float]] = Array(Array(1.0f, 1.0f, 1.0f), Array(0.0f, 0.0f, 0.0f), Array(0.0f, 0.0f, 0.0f))
  val weightsL3: Array[Array[Float]] = Array(Array(1.0f, 0.9f, 0.8f), Array(0.7f, 2f, 0.4f), Array(3f, 0.2f, 0.1f))
  val biasesL3: Array[Array[Float]] = Array(Array(1.0f, 1.0f, 1.0f), Array(1.0f, 0.0f, 1.0f), Array(1.0f, 1.0f, 1.0f))
  val signL3: Int = sign
  val fixedPointL3: Int = fix

  val inputs: Array[Array[Array[Int]]] = Array(
    FixedPointConversion.convertFloatMatrixToFixedMatrix(inputsL1, fixedPointL1, w, signL1 == 1),
    FixedPointConversion.convertFloatMatrixToFixedMatrix(inputsL2, fixedPointL2, w, signL2 == 1),
    FixedPointConversion.convertFloatMatrixToFixedMatrix(inputsL3, fixedPointL3, w, signL3 == 1)
  )
  val weights: Array[Array[Array[Int]]] = Array(
    FixedPointConversion.convertFloatMatrixToFixedMatrix(weightsL1, fixedPointL1, w, signL1 == 1),
    FixedPointConversion.convertFloatMatrixToFixedMatrix(weightsL2, fixedPointL2, w, signL2 == 1),
    FixedPointConversion.convertFloatMatrixToFixedMatrix(weightsL3, fixedPointL3, w, signL3 == 1)
  )
  val biases: Array[Array[Array[Int]]] = Array(
    FixedPointConversion.convertFloatMatrixToFixedMatrix(biasesL1, fixedPointL1, wStore, signL1 == 1),
    FixedPointConversion.convertFloatMatrixToFixedMatrix(biasesL2, fixedPointL2, wStore, signL2 == 1),
    FixedPointConversion.convertFloatMatrixToFixedMatrix(biasesL3, fixedPointL3, wStore, signL3 == 1)
  )

  val signs: Array[Int] = Array(signL1, signL2, signL3)
  val fixedPoints: Array[Int] = Array(fixedPointL1, fixedPointL2, fixedPointL3)

  var mappedInputs = Mapping.mapInputs(inputs)
  var mappedWeights = Mapping.mapWeights(weights)
  var mappedBiases = Mapping.mapBiases(biases)

  "Should initially set address to 0, then increment to 1 after one increment message via UART." in {
    test(new Accelerator(w, wStore, dimension, dimension, mappedInputs, mappedWeights, mappedBiases, signs, fixedPoints, true)) { dut =>
      dut.io.debugAddress.get.expect(0)
      dut.clock.step()
      for (i <- 0 until dimension * dimension) {
        print(FixedPointConversion.fixedToFloat(dut.io.dataOutW(i).peekInt().toInt, fixedPointL1, w, sign == 1).toString() + " ")
      }
      println()
      dut.io.startCalculation.poke(true.B)
      while (!dut.io.calculationDone.peek().litToBoolean) {
        dut.clock.step()
      }
      dut.io.startCalculation.poke(false.B)
      dut.io.debugAddress.get.expect(1)
      dut.io.readEnable.poke(true.B)
      dut.clock.step()
      for (i <- 0 until dimension * dimension) {
        print(FixedPointConversion.fixedToFloat(dut.io.dataOutW(i).peekInt().toInt, fixedPointL1, w, sign == 1).toString() + " ")
      }
      println()
      dut.io.readEnable.poke(false.B)
      dut.clock.step()
      dut.io.startCalculation.poke(true.B)
      while (!dut.io.calculationDone.peek().litToBoolean) {
        dut.clock.step()
      }
      dut.io.startCalculation.poke(false.B)
      dut.io.debugAddress.get.expect(2)
      dut.io.readEnable.poke(true.B)
      dut.clock.step()
      for (i <- 0 until dimension * dimension) {
        print(FixedPointConversion.fixedToFloat(dut.io.dataOutW(i).peekInt().toInt, fixedPointL1, w, sign == 1).toString() + " ")
      }
      println()
      dut.io.readEnable.poke(false.B)
      dut.clock.step()
      dut.io.startCalculation.poke(true.B)
      while (!dut.io.calculationDone.peek().litToBoolean) {
        dut.clock.step()
      }
      dut.io.startCalculation.poke(false.B)
      dut.io.debugAddress.get.expect(0)
      dut.io.readEnable.poke(true.B)
      dut.clock.step()
      for (i <- 0 until dimension * dimension) {
        print(FixedPointConversion.fixedToFloat(dut.io.dataOutW(i).peekInt().toInt, fixedPointL1, w, sign == 1).toString() + " ")
      }
      println()
    }
  }


}