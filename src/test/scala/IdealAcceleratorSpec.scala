import Utils.MatrixUtils._
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class IdealAcceleratorSpec extends AnyFreeSpec with ChiselScalatestTester {

  val clockTimeout = 200_000_000
  val frequency = 100
  val baudRate = 1
  val cyclesPerSerialBit = Utils.UartCoding.cyclesPerSerialBit(frequency, baudRate)
  val w = 8

  val dimension = 3

  val inputsL1: Array[Array[Float]] = Array(Array(1.2f, 1.3f, 2.4f), Array(0.9f, 3.4f, 0.9f), Array(2.2f, 1.2f, 0.9f))
  val weightsL1: Array[Array[Float]] = Array(Array(2.2f, 1.3f, 1.0f), Array(4.9f, 0.4f, 4.8f), Array(2.2f, 1.2f, 0.9f))
  val biasesL1: Array[Array[Float]] = Array(Array(1.0f, 1.0f, 1.0f), Array(1.0f, 1.0f, 1.0f), Array(1.0f, 1.0f, 1.0f))
  val signL1: Int = 0
  val fixedPointL1: Int = 0

  val inputsL2: Array[Array[Float]] = Array(Array(0.0f, 0.0f, 0.0f), Array(0.0f, 0.0f, 0.0f), Array(0.0f, 0.0f, 0.0f))
  val weightsL2: Array[Array[Float]] = Array(Array(1.0f, 0.9f, 0.8f), Array(0.7f, 0.6f, 0.4f), Array(0.3f, 0.2f, 0.1f))
  val biasesL2: Array[Array[Float]] = Array(Array(0.0f, 0.0f, 0.0f), Array(1.0f, 1.0f, 1.0f), Array(1.0f, 1.0f, 1.0f))
  val signL2: Int = 0
  val fixedPointL2: Int = 0

  /*
  val dimension = 4

  val inputsL1: Array[Array[Float]] = Array(Array(1.2f, 1.3f, 2.4f, 1.0f), Array(0.9f, 3.4f, 0.9f, 2.2f), Array(2.2f, 1.2f, 0.9f, 1.2f), Array(2.2f, 1.2f, 0.9f, 1.2f))
  val weightsL1: Array[Array[Float]] = Array(Array(2.2f, 1.3f, 1.0f, 0.2f), Array(4.9f, 0.4f, 4.8f, 4.2f), Array(2.2f, 1.2f, 0.9f, 1.2f), Array(2.2f, 1.2f, 0.9f, 1.2f))
  val biasesL1: Array[Array[Float]] = Array(Array(1.0f, 1.0f, 1.0f, 1.0f), Array(1.0f, 1.0f, 1.0f, 1.0f), Array(1.0f, 1.0f, 1.0f, 1.0f), Array(1.0f, 1.0f, 1.0f, 1.0f))
  val signL1: Int = 0
  val fixedPointL1: Int = 2

  val inputsL2: Array[Array[Float]] = Array(Array(0.0f, 0.0f, 0.0f, 0.0f), Array(0.0f, 0.0f, 0.0f, 0.0f), Array(0.0f, 0.0f, 0.0f, 0.0f), Array(0.0f, 0.0f, 0.0f, 0.0f))
  val weightsL2: Array[Array[Float]] = Array(Array(0.0f, 0.0f, 0.0f, 0.0f), Array(0.0f, 0.0f, 0.0f, 0.0f), Array(0.0f, 0.0f, 0.0f, 0.0f), Array(0.0f, 0.0f, 0.0f, 0.0f))
  val biasesL2: Array[Array[Float]] = Array(Array(0.0f, 0.0f, 0.0f, 0.0f), Array(0.0f, 0.0f, 0.0f, 0.0f), Array(0.0f, 0.0f, 0.0f, 0.0f), Array(0.0f, 0.0f, 0.0f, 0.0f))
  val signL2: Int = 0
  val fixedPointL2: Int = 2
  */


  val inputs: Array[Array[Array[Int]]] = Array(
    Configuration.convertFloatMatrixToFixedMatrix(inputsL1, fixedPointL1),
    Configuration.convertFloatMatrixToFixedMatrix(inputsL2, fixedPointL2)
  )
  val weights: Array[Array[Array[Int]]] = Array(
    Configuration.convertFloatMatrixToFixedMatrix(weightsL1, fixedPointL1),
    Configuration.convertFloatMatrixToFixedMatrix(weightsL2, fixedPointL2)
  )
  val biases: Array[Array[Array[Int]]] = Array(
    Configuration.convertFloatMatrixToFixedMatrix(biasesL1, fixedPointL1),
    Configuration.convertFloatMatrixToFixedMatrix(biasesL2, fixedPointL2)
  )

  val signs: Array[Int] = Array(signL1, signL2)
  val fixedPoints: Array[Int] = Array(fixedPointL1, fixedPointL2)

  var mappedInputs = Configuration.mapInputs(inputs)
  var mappedWeights = Configuration.mapWeights(weights)
  var mappedBiases = Configuration.mapBiases(biases)

  // 0011 in decimal: 3

  val nextInputsOpcode = 1.toByte
  val nextCalculatingOpcode = 3.toByte
  val incrementAddressOpcode = 4.toByte

  "Should initially set address to 0, then increment to 1 after one increment message via UART." in {
    test(new IdealAccelerator(w, dimension, frequency, baudRate, mappedInputs, mappedWeights, mappedBiases, signs, fixedPoints)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      dut.io.address.expect(0.U)

      var memoryValues1 = Array.fill(dimension * dimension)(0)
      var memoryValues2 = Array.fill(dimension * dimension)(0)
      var memoryValues3 = Array.fill(dimension * dimension)(0)

      dut.io.readDebug.poke(true.B)
      for (i <- 0 until dimension * dimension) {
        memoryValues1(i) = dut.io.debugMatrixMemory1(i).peekInt().toInt
        memoryValues2(i) = dut.io.debugMatrixMemory2(i).peekInt().toInt
        memoryValues3(i) = dut.io.debugMatrixMemory3(i).peekInt().toInt
      }
      println("-- inputs --")
      print(matrixToString(convertMappedMatrixToMatrix(memoryValues1, dimension)))
      println("-- weights --")
      print(matrixToString(convertMappedMatrixToMatrix(memoryValues2, dimension)))
      println("-- biases --")
      print(matrixToString(convertMappedMatrixToMatrix(memoryValues3, dimension)))
      dut.io.readDebug.poke(false.B)
      println("-------")

      dut.io.rxd.poke(1.U(1.W)) // UART idle signal is high
      dut.clock.step(100)

      val bytesToSend = Array(
        incrementAddressOpcode
      )

      val uartBitsToSend = Utils.UartCoding.encodeBytesToUartBits(bytesToSend)

      println("Sending bit vector: " + uartBitsToSend)

      uartBitsToSend.foreach(bit => {
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))
        dut.clock.step(cyclesPerSerialBit)
      })

      dut.io.address.expect(1.U)
      dut.io.readDebug.poke(true.B)
      for (i <- 0 until dimension * dimension) {
        memoryValues1(i) = dut.io.debugMatrixMemory1(i).peekInt().toInt
        memoryValues2(i) = dut.io.debugMatrixMemory2(i).peekInt().toInt
        memoryValues3(i) = dut.io.debugMatrixMemory3(i).peekInt().toInt
      }
      println("-- inputs --")
      print(matrixToString(convertMappedMatrixToMatrix(memoryValues1, dimension)))
      println("-- weights --")
      print(matrixToString(convertMappedMatrixToMatrix(memoryValues2, dimension)))
      println("-- biases --")
      print(matrixToString(convertMappedMatrixToMatrix(memoryValues3, dimension)))
      dut.io.readDebug.poke(false.B)
      println("-------")

      println("TEST 1 DONE")
      println("-------")
    }

  }
  "Should start the calculation" in {
    test(new IdealAccelerator(w, dimension, frequency, baudRate, mappedInputs, mappedWeights, mappedBiases, signs, fixedPoints)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      dut.io.address.expect(0.U)

      var memoryValues1 = Array.fill(dimension * dimension)(0)
      var memoryValues2 = Array.fill(dimension * dimension)(0)
      var memoryValues3 = Array.fill(dimension * dimension)(0)

      dut.io.readDebug.poke(true.B)
      for (i <- 0 until dimension * dimension) {
        memoryValues1(i) = dut.io.debugMatrixMemory1(i).peekInt().toInt
        memoryValues2(i) = dut.io.debugMatrixMemory2(i).peekInt().toInt
        memoryValues3(i) = dut.io.debugMatrixMemory3(i).peekInt().toInt
      }
      println("-- inputs --")
      print(matrixToString(convertMappedMatrixToMatrix(memoryValues1, dimension)))
      println("-- weights --")
      print(matrixToString(convertMappedMatrixToMatrix(memoryValues2, dimension)))
      println("-- biases --")
      print(matrixToString(convertMappedMatrixToMatrix(memoryValues3, dimension)))
      dut.io.readDebug.poke(false.B)
      println("-------")

      dut.io.rxd.poke(1.U(1.W)) // UART idle signal is high
      dut.clock.step(100)

      val bytesToSend = Array(
        nextCalculatingOpcode
      )
      val uartBitsToSend = Utils.UartCoding.encodeBytesToUartBits(bytesToSend)
      println("Sending bit vector: " + uartBitsToSend)
      uartBitsToSend.foreach(bit => {
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))
        dut.clock.step(cyclesPerSerialBit)
      })

      dut.clock.step(dimension * dimension + 1) //let systolic run!
      //dut.io.address.expect(1.U)
      println("Evaluated into:")
      println(dut.io.address.peekInt())
      println(dut.io.matrixAddress.peekInt())

      dut.io.readDebug.poke(true.B)
      println(dut.io.ready.peek())
      for (i <- 0 until dimension * dimension) {
        memoryValues1(i) = dut.io.debugMatrixMemory1(i).peekInt().toInt
        memoryValues2(i) = dut.io.debugMatrixMemory2(i).peekInt().toInt
        memoryValues3(i) = dut.io.debugMatrixMemory3(i).peekInt().toInt
      }
      println("-- inputs --")
      print(matrixToString(convertMappedMatrixToMatrix(memoryValues1, dimension)))
      println("-- weights --")
      print(matrixToString(convertMappedMatrixToMatrix(memoryValues2, dimension)))
      println("-- biases --")
      print(matrixToString(convertMappedMatrixToMatrix(memoryValues3, dimension)))
      dut.io.readDebug.poke(false.B)


      val newBytesToSend = Array(
        nextCalculatingOpcode
      )

      val newUartBitsToSend = Utils.UartCoding.encodeBytesToUartBits(newBytesToSend)
      println("Sending bit vector: " + newUartBitsToSend)
      newUartBitsToSend.foreach(bit => {
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))
        dut.clock.step(cyclesPerSerialBit)
      })

      println("Evaluated into:")
      println(dut.io.address.peekInt())
      println(dut.io.matrixAddress.peekInt())

      dut.io.readDebug.poke(true.B)
      println(dut.io.ready.peek())
      for (i <- 0 until dimension * dimension) {
        memoryValues1(i) = dut.io.debugMatrixMemory1(i).peekInt().toInt
        memoryValues2(i) = dut.io.debugMatrixMemory2(i).peekInt().toInt
        memoryValues3(i) = dut.io.debugMatrixMemory3(i).peekInt().toInt
      }
      println("-- inputs --")
      print(matrixToString(convertMappedMatrixToMatrix(memoryValues1, dimension)))
      println("-- weights --")
      print(matrixToString(convertMappedMatrixToMatrix(memoryValues2, dimension)))
      println("-- biases --")
      print(matrixToString(convertMappedMatrixToMatrix(memoryValues3, dimension)))
      dut.io.readDebug.poke(false.B)

      println("-------")
      println("TEST 2 DONE")
      println("-------")


    }
  }
}