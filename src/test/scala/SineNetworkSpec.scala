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
  val fixedPoint = 4
  val signed = true

  // convert to fixed point using the same fixed point and sign for all layers
  val weightsL1Fixed = convertFloatMatrixToFixedMatrix(weightsL1, fixedPoint, w, signed)
  val biasesL1Fixed = convertFloatMatrixToFixedMatrix(biasesL1, fixedPoint, wResult, signed)
  val weightsL2Fixed = convertFloatMatrixToFixedMatrix(weightsL2, fixedPoint, w, signed)
  val biasesL2Fixed = convertFloatMatrixToFixedMatrix(biasesL2, fixedPoint, wResult, signed)
  val weightsL3Fixed = convertFloatMatrixToFixedMatrix(weightsL3, fixedPoint, w, signed)
  val biasesL3Fixed = convertFloatMatrixToFixedMatrix(biasesL3, fixedPoint, wResult, signed)

  // print the fixed point weights and biases
  println("w1:" + weightsL1Fixed.map(_.mkString("Array(", ", ", ")")).mkString("Array(", ", ", ")"))
  println("b1:" + biasesL1Fixed.map(_.mkString("Array(", ", ", ")")).mkString("Array(", ", ", ")"))
  println("w2:" + weightsL2Fixed.map(_.mkString("Array(", ", ", ")")).mkString("Array(", ", ", ")"))
  println("b2:" + biasesL2Fixed.map(_.mkString("Array(", ", ", ")")).mkString("Array(", ", ", ")"))
  println("w3:" + weightsL3Fixed.map(_.mkString("Array(", ", ", ")")).mkString("Array(", ", ", ")"))
  println("b3:" + biasesL3Fixed.map(_.mkString("Array(", ", ", ")")).mkString("Array(", ", ", ")"))

  // map the weights to the correct shifted format


  // collect the weights and biases into arrays
  val weights = Array(weightsL1Fixed, weightsL2Fixed, weightsL3Fixed)
  val biases = Array(biasesL1Fixed, biasesL2Fixed, biasesL3Fixed)

  "SineNetwork should behave correctly" in {
    test(new SineNetwork(w, wResult, signed, fixedPoint, weights, biases, true)) { dut =>
      dut.io.load.poke(true.B)
      dut.io.input.poke(1.U)
      dut.clock.step(1)
      dut.io.load.poke(false.B)
      dut.io.output.expect(0.U)
    }
  }

}
