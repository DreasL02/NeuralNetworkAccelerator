import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class SinusAcceleratorSpec extends AnyFreeSpec with ChiselScalatestTester {
  /*
  val w = 8
  val fix = 6
  val dimension = 16
  val sign = 1
  val layers = 3

  val inputs = Array.ofDim[Float](layers, dimension, dimension)
  val fixedInputs : Array[Array[Array[Int]]] = Array.ofDim[Int](layers, dimension, dimension)
  for(i <- 0 until layers) {
    inputs(i) = FileReader.upscaleMatrixToDimension(Array(Array(0.3f)), dimension)
    fixedInputs(i) = Configuration.convertFloatMatrixToFixedMatrix(inputs(i), fix, w)
    print(MatrixUtils.matrixToString(inputs(i)))
    println()
    print(MatrixUtils.matrixToString(fixedInputs(i)))
    println()
  }

  val weights = Array.ofDim[Float](layers, dimension, dimension)
  val fixedWeights : Array[Array[Array[Int]]] = Array.ofDim[Int](layers, dimension, dimension)
  for(i <- 0 until layers) {
    val filename = "src/test/scala/Utils/data/matlab-inference-data_w" + (i+1) + ".txt"
    weights(i) = FileReader.upscaleMatrixToDimension(FileReader.readMatrixFromFile(filename), dimension)
    fixedWeights(i) = Configuration.convertFloatMatrixToFixedMatrix(weights(i), fix, w)
    print(MatrixUtils.matrixToString(weights(i)))
    println()
    print(MatrixUtils.matrixToString(fixedWeights(i)))
    println()
  }
  val biases = Array.ofDim[Float](layers, dimension, dimension)
  val fixedBiases : Array[Array[Array[Int]]] = Array.ofDim[Int](layers, dimension, dimension)
  for(i <- 0 until layers) {
    val filename = "src/test/scala/Utils/data/matlab-inference-data_b" + (i+1) + ".txt"
    biases(i) = FileReader.upscaleMatrixToDimension(FileReader.readMatrixFromFile(filename), dimension)
    fixedBiases(i) = Configuration.convertFloatMatrixToFixedMatrix(biases(i), fix, w)
    print(MatrixUtils.matrixToString(biases(i)))
    println()
    print(MatrixUtils.matrixToString(fixedBiases(i)))
    println()
  }

  val signs: Array[Int] = Array.ofDim[Int](layers)
  for(i <- 0 until layers) {
    signs(i) = sign
  }

  val fixedPoints: Array[Int] = Array.ofDim[Int](layers)
  for(i <- 0 until layers) {
    fixedPoints(i) = fix
  }


  println("Mapped")
  var mappedInputs = Configuration.mapInputs(fixedInputs)
  var mappedWeights = Configuration.mapWeights(fixedWeights)
  var mappedBiases = Configuration.mapBiases(fixedBiases)
  for(i <- 0 until dimension * dimension * layers) {
      print(mappedInputs(i).toString() + " ")
  }
  println()
  for(i <- 0 until dimension * dimension * layers) {
      print(mappedWeights(i).toString() + " ")
  }
  println()
  for(i <- 0 until dimension * dimension * layers) {
      print(mappedBiases(i).toString() + " ")
  }
  println()

  "Should initially set address to 0, then increment to 1 after one increment message via UART." in {
    test(new Accelerator(w, dimension, mappedInputs, mappedWeights, mappedBiases, signs, fixedPoints, true)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      println("Sine of 0.3 is 0.2955202066613396")
      println("Testing begun")
      dut.io.address.get.expect(0)
      dut.io.readEnable.poke(true.B)
      dut.clock.step()
      println("Initial read output (should be all zeroes)")
      for (i <- 0 until dimension * dimension) {
        print(Configuration.fixedToFloat(dut.io.dataOutW(i).peekInt().toInt, fix).toString() + " ")
      }
      dut.io.readEnable.poke(false.B)
      println()
      dut.clock.step()

      for (i <- 0 until layers) {
        println("Starting calc of layer " + (i+1))
        dut.io.startCalculation.poke(true.B)
        // var bob = 0;
        while (!dut.io.calculationDone.peek().litToBoolean) {
          // println("Cycle " + (bob + 1) + " of layer " + (i+1))
          // for (i <- 0 until dimension * dimension) {
          //     print(dut.io.dataOutW(i).peekInt().toString() + " ")
          //     print(Configuration.fixedToFloat(dut.io.dataOutW(i).peekInt().toInt, fix).toString() + " ")
          // }
          // println()
          // bob = bob + 1
          dut.clock.step()
        }
        println("Done with calc of layer " + (i+1))
        dut.io.startCalculation.poke(false.B)
        dut.io.readEnable.poke(true.B)
        println()
        dut.clock.step()
        println("Results from calc of layer " + (i+1))
        for (i <- 0 until dimension * dimension) {
          print(dut.io.dataOutW(i).peekInt().toString() + " ")
          print(Configuration.fixedToFloat(dut.io.dataOutW(i).peekInt().toInt, fix).toString() + " ")
        }
        println()
        dut.io.readEnable.poke(false.B)
        dut.clock.step()
      }
    }
  }

   */
}
