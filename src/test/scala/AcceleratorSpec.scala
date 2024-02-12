
import utils.{FixedPointConversion, Mapping}
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import utils.MatrixUtils.matrixToString
import utils.RandomData.randomMatrix

class AcceleratorSpec extends AnyFreeSpec with ChiselScalatestTester {
  // ======= configure the test =======
  val w = 8
  val wStore = 4 * w
  val xDimension = 3
  val yDimension = xDimension // Only square matrices for now
  val matrixCommonDimension = 3
  val layers = 2
  val fixedPoint = 0
  val signed = true.B
  val numberOfTests = 1
  val max = 1.2f
  val min = -1.2f //0.0f //
  val threshold = 1f

  val printing = Array.fill(numberOfTests)(false)

  // We can enable printing for a specific test by setting the index to true
  printing(0) = true

  // ======= configure the test end =======

  // --- Rest should be left as is ---
  val seeds = Array.fill(numberOfTests * 3 * layers)(0)
  // increment seeds for each test to get different random numbers
  for (i <- 0 until numberOfTests * 3 * layers) {
    seeds(i) = 10 * i
  }

  // for each seed, generate a random matrix and test
  for (testNum <- 0 until numberOfTests) {
    val enablePrinting = printing(testNum)

    val inputsFloat = Array.ofDim[Array[Array[Float]]](layers)
    val weightsFloat = Array.ofDim[Array[Array[Float]]](layers)
    val biasesFloat = Array.ofDim[Array[Array[Float]]](layers)

    val inputs = Array.ofDim[Array[Array[BigInt]]](layers)
    val weights = Array.ofDim[Array[Array[BigInt]]](layers)
    val biases = Array.ofDim[Array[Array[BigInt]]](layers)
    val signs = Array.ofDim[BigInt](layers)
    val fixedPoints = Array.ofDim[BigInt](layers)

    // for each layer, generate a random set matrices
    for (layer <- 0 until layers) {
      inputsFloat(layer) = randomMatrix(yDimension, matrixCommonDimension, min, max, seeds(testNum * 3 * layers + layer * 3))
      weightsFloat(layer) = randomMatrix(matrixCommonDimension, xDimension, min, max, seeds(testNum * 3 * layers + layer * 3 + 1))
      biasesFloat(layer) = randomMatrix(xDimension, yDimension, min, max, seeds(testNum * 3 * layers + layer * 3 + 2))

      if (enablePrinting) {
        println("inputsFloat for layer %d".format(layer))
        print(matrixToString(inputsFloat(layer)))
        println("weightsFloat for layer %d".format(layer))
        print(matrixToString(weightsFloat(layer)))
        println("biasesFloat for layer %d".format(layer))
        print(matrixToString(biasesFloat(layer)))
        println();
      }

      inputs(layer) = FixedPointConversion.convertFloatMatrixToFixedMatrix(inputsFloat(layer), fixedPoint, w, signed.litToBoolean)
      weights(layer) = FixedPointConversion.convertFloatMatrixToFixedMatrix(weightsFloat(layer), fixedPoint, w, signed.litToBoolean)
      biases(layer) = FixedPointConversion.convertFloatMatrixToFixedMatrix(biasesFloat(layer), fixedPoint * 2, wStore, signed.litToBoolean)

      if (enablePrinting) {
        println("inputs for layer %d".format(layer))
        print(matrixToString(inputs(layer)))
        println("weights for layer %d".format(layer))
        print(matrixToString(weights(layer)))
        println("biases for layer %d".format(layer))
        print(matrixToString(biases(layer)))
        println();
      }

      signs(layer) = signed.litValue
      fixedPoints(layer) = BigInt(fixedPoint)
    }

    val mappedInputs = Mapping.mapInputs(inputs)
    val mappedWeights = Mapping.mapWeights(weights)
    val mappedBiases = Mapping.mapBiases(biases)

    if (enablePrinting) {
      println("mappedInputs")
      print(matrixToString(mappedInputs))
      println("mappedWeights")
      print(matrixToString(mappedWeights))
      println("mappedBiases")
      print(matrixToString(mappedBiases))
      println();
    }

    "AcceleratorSpec should calculate correctly for test %d".format(testNum) in {
      test(new Accelerator(w, wStore, xDimension, yDimension, mappedInputs, mappedWeights, mappedBiases, signs, fixedPoints, true)) { dut =>

        dut.io.debugAddress.get.expect(0)
        dut.clock.step()
        if (enablePrinting) {
          for (i <- 0 until xDimension * yDimension) {
            print(FixedPointConversion.fixedToFloat(dut.io.dataOutW(i).peekInt().toInt, fixedPoints(0).toInt, w, signs(0) == 1).toString() + " ")
          }
          println()
        }
        println()

        // for each layer
        for (layer <- 0 until layers) {
          dut.io.startCalculation.poke(true.B) // start the calculation
          while (!dut.io.calculationDone.peek().litToBoolean) { // wait for the calculation to finish
            dut.clock.step()
          }
          dut.io.startCalculation.poke(false.B)
          dut.io.readEnable.poke(true.B) // read the result
          dut.clock.step() // wait for the result to be ready
          if (enablePrinting) {
            for (i <- 0 until xDimension * yDimension) {
              print(FixedPointConversion.fixedToFloat(dut.io.dataOutW(i).peekInt().toInt, fixedPoints(layer).toInt, w, signs(layer) == 1).toString() + " ")
            }
            println()
          }
          println()
          dut.io.readEnable.poke(false.B)
          dut.clock.step()
        }
      }
    }
  }


}