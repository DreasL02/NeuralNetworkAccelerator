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
  val biasesL1Fixed = convertFloatMatrixToFixedMatrix(biasesL1, fixedPoint * 2, wResult, signed)
  val weightsL2Fixed = convertFloatMatrixToFixedMatrix(weightsL2, fixedPoint, w, signed)
  val biasesL2Fixed = convertFloatMatrixToFixedMatrix(biasesL2, fixedPoint * 2, wResult, signed)
  val weightsL3Fixed = convertFloatMatrixToFixedMatrix(weightsL3, fixedPoint, w, signed)
  val biasesL3Fixed = convertFloatMatrixToFixedMatrix(biasesL3, fixedPoint * 2, wResult, signed)

  // collect the weights and biases into arrays
  val weights = Array(weightsL1Fixed, weightsL2Fixed, weightsL3Fixed)
  val biases = Array(biasesL1Fixed, biasesL2Fixed, biasesL3Fixed)

  val input = 1.0f
  val inputFixed = floatToFixed(input, fixedPoint, w, signed)

  val print1 = false // seems to work
  val print2 = true
  val print3 = false

  "SineNetwork should behave correctly" in {
    test(new SineNetwork(w, wResult, signed, fixedPoint, weights, biases, true)) { dut =>
      dut.io.loads(0).poke(true.B)
      dut.io.loads(1).poke(false.B)
      dut.io.loads(2).poke(false.B)

      dut.io.input.poke(inputFixed.U)

      dut.clock.step()
      dut.io.loads(0).poke(false.B)
      var cycle = 0
      while (!dut.io.valids(0).peekBoolean()) {
        if (print1) {
          println("Cycle: " + cycle)
          println("MMU1 Input")
          for (i <- 0 until dut.io.debugMMU1Input.get.length) {
            print(fixedToFloat(dut.io.debugMMU1Input.get(i).peek().litValue, fixedPoint, w, signed))
            print(" ")
          }
          println()
          println("MMU1 Weights")
          for (i <- 0 until dut.io.debugMMU1Weights.get.length) {
            print(fixedToFloat(dut.io.debugMMU1Weights.get(i).peek().litValue, fixedPoint, w, signed))
            print(" ")
          }
          println()
          println("MMU1 Result")
          for (i <- 0 until dut.io.debugMMU1Result.get.length) {
            for (j <- 0 until dut.io.debugMMU1Result.get(i).length) {
              print(fixedToFloat(dut.io.debugMMU1Result.get(i)(j).peek().litValue, fixedPoint * 2, w * 2, signed))
              print(" ")
            }
            println()
          }
          println("Bias1 Biases")
          for (i <- 0 until dut.io.debugBias1Biases.get.length) {
            for (j <- 0 until dut.io.debugBias1Biases.get(i).length) {
              print(fixedToFloat(dut.io.debugBias1Biases.get(i)(j).peek().litValue, fixedPoint * 2, w * 2, signed))
              print(" ")
            }
            println()
          }
          println("Rounder Input / Bias result")
          for (i <- 0 until dut.io.debugRounder1Input.get.length) {
            for (j <- 0 until dut.io.debugRounder1Input.get(i).length) {
              print(fixedToFloat(dut.io.debugRounder1Input.get(i)(j).peek().litValue, fixedPoint * 2, w * 2, signed))
              print(" ")
            }
            println()
          }
          println("Rounder1 Output")
          for (i <- 0 until dut.io.debugRounder1Output.get.length) {
            for (j <- 0 until dut.io.debugRounder1Output.get(i).length) {
              print(fixedToFloat(dut.io.debugRounder1Output.get(i)(j).peek().litValue, fixedPoint, w, signed))
              print(" ")
            }
            println()
          }
          println("ReLU1 Input")
          for (i <- 0 until dut.io.debugReLU1Input.get.length) {
            for (j <- 0 until dut.io.debugReLU1Input.get(i).length) {
              print(fixedToFloat(dut.io.debugReLU1Input.get(i)(j).peek().litValue, fixedPoint, w, signed))
              print(" ")
            }
            println()
          }
          println("ReLU1 Output")
          for (i <- 0 until dut.io.debugReLU1Output.get.length) {
            for (j <- 0 until dut.io.debugReLU1Output.get(i).length) {
              print(fixedToFloat(dut.io.debugReLU1Output.get(i)(j).peek().litValue, fixedPoint, w, signed))
              print(" ")
            }
            println()
          }


        }
        dut.clock.step()
        cycle += 1
      }

      println("ReLU1 Output")
      for (i <- 0 until dut.io.debugReLU1Output.get.length) {
        for (j <- 0 until dut.io.debugReLU1Output.get(i).length) {
          print(fixedToFloat(dut.io.debugReLU1Output.get(i)(j).peek().litValue, fixedPoint, w, signed))
          print(" ")
        }
        println()
      }

      dut.io.loads(1).poke(true.B)
      dut.clock.step()
      dut.io.loads(1).poke(false.B)
      cycle = 0
      while (!dut.io.valids(1).peekBoolean()) {
        if (print2) {
          println("Cycle: " + cycle)
          println("MMU2 Input")
          for (i <- 0 until dut.io.debugMMU2Input.get.length) {
            print(dut.io.debugMMU2Input.get(i).peek().litValue)
            print(" ")
          }
          println()
          println("MMU2 Weights")
          for (i <- 0 until dut.io.debugMMU2Weights.get.length) {
            print(dut.io.debugMMU2Weights.get(i).peek().litValue)
            print(" ")
          }
          println()
          println("MMU2 Result")
          for (i <- 0 until dut.io.debugMMU2Result.get.length) {
            for (j <- 0 until dut.io.debugMMU2Result.get(i).length) {
              print(fixedToFloat(dut.io.debugMMU2Result.get(i)(j).peek().litValue, fixedPoint * 2, w * 2, signed))
              print(" ")
            }
            println()
          }
          println("Bias2 Biases")
          for (i <- 0 until dut.io.debugBias2Biases.get.length) {
            for (j <- 0 until dut.io.debugBias2Biases.get(i).length) {
              print(fixedToFloat(dut.io.debugBias2Biases.get(i)(j).peek().litValue, fixedPoint * 2, w * 2, signed))
              print(" ")
            }
            println()
          }
          println("Rounder2 Input / Bias result")
          for (i <- 0 until dut.io.debugRounder2Input.get.length) {
            for (j <- 0 until dut.io.debugRounder2Input.get(i).length) {
              print(fixedToFloat(dut.io.debugRounder2Input.get(i)(j).peek().litValue, fixedPoint * 2, w * 2, signed))
              print(" ")
            }
            println()
          }
          println("Rounder2 Output")
          for (i <- 0 until dut.io.debugRounder2Output.get.length) {
            for (j <- 0 until dut.io.debugRounder2Output.get(i).length) {
              print(fixedToFloat(dut.io.debugRounder2Output.get(i)(j).peek().litValue, fixedPoint, w, signed))
              print(" ")
            }
            println()
          }
          println("ReLU2 Input")
          for (i <- 0 until dut.io.debugReLU2Input.get.length) {
            for (j <- 0 until dut.io.debugReLU2Input.get(i).length) {
              print(fixedToFloat(dut.io.debugReLU2Input.get(i)(j).peek().litValue, fixedPoint, w, signed))
              print(" ")
            }
            println()
          }
          println("ReLU2 Output")
          for (i <- 0 until dut.io.debugReLU2Output.get.length) {
            for (j <- 0 until dut.io.debugReLU2Output.get(i).length) {
              print(fixedToFloat(dut.io.debugReLU2Output.get(i)(j).peek().litValue, fixedPoint, w, signed))
              print(" ")
            }
            println()
          }
        }
        dut.clock.step()
        cycle += 1
      }

      println("MMU2 Result")
      for (i <- 0 until dut.io.debugMMU2Result.get.length) {
        for (j <- 0 until dut.io.debugMMU2Result.get(i).length) {
          print(fixedToFloat(dut.io.debugMMU2Result.get(i)(j).peek().litValue, fixedPoint * 2, w * 2, signed))
          print(" ")
        }
        println()
      }


      dut.io.loads(2).poke(true.B)
      dut.clock.step()
      dut.io.loads(2).poke(false.B)
      cycle = 0
      while (!dut.io.valids(2).peekBoolean()) {
        if (print3) {
          println("Cycle: " + cycle)
          println("MMU3 Input")
          for (i <- 0 until dut.io.debugMMU3Input.get.length) {
            print(dut.io.debugMMU3Input.get(i).peek().litValue)
            print(" ")
          }
          println()
          println("MMU3 Weights")
          for (i <- 0 until dut.io.debugMMU3Weights.get.length) {
            print(dut.io.debugMMU3Weights.get(i).peek().litValue)
            print(" ")
          }
          println()
          println("MMU3 Result")
          for (i <- 0 until dut.io.debugMMU3Result.get.length) {
            for (j <- 0 until dut.io.debugMMU3Result.get(i).length) {
              print(fixedToFloat(dut.io.debugMMU3Result.get(i)(j).peek().litValue, fixedPoint * 2, w * 2, signed))
              print(" ")
            }
            println()
          }
        }
        cycle += 1
        dut.clock.step()
      }

      println("MMU3 Result")
      for (i <- 0 until dut.io.debugMMU3Result.get.length) {
        for (j <- 0 until dut.io.debugMMU3Result.get(i).length) {
          print(fixedToFloat(dut.io.debugMMU3Result.get(i)(j).peek().litValue, fixedPoint * 2, w * 2, signed))
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
