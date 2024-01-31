import Utils.MatrixUtils
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

import scala.collection.mutable.ListBuffer

class UartAcceleratorSpec extends AnyFreeSpec with ChiselScalatestTester {

  val clockTimeout = 200_000_000
  val frequency = 100
  val baudRate = 1
  val dimension = 3
  val cyclesPerSerialBit = Utils.UartCoding.cyclesPerSerialBit(frequency, baudRate)

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

  val inputs: Array[Array[Array[Int]]] = Array(
    Configuration.convertFloatMatrixToFixedMatrix(inputsL1, fixedPointL1),
    Configuration.convertFloatMatrixToFixedMatrix(inputsL2, fixedPointL2),
  )
  val weights: Array[Array[Array[Int]]] = Array(
    Configuration.convertFloatMatrixToFixedMatrix(weightsL1, fixedPointL1),
    Configuration.convertFloatMatrixToFixedMatrix(weightsL2, fixedPointL2),
  )
  val biases: Array[Array[Array[Int]]] = Array(
    Configuration.convertFloatMatrixToFixedMatrix(biasesL1, fixedPointL1),
    Configuration.convertFloatMatrixToFixedMatrix(biasesL2, fixedPointL2),
  )

  val signs: Array[Int] = Array(signL1, signL2)
  val fixedPoints: Array[Int] = Array(fixedPointL1, fixedPointL2)

  var mappedInputs = Configuration.mapInputs(inputs)
  var mappedWeights = Configuration.mapWeights(weights)
  var mappedBiases = Configuration.mapBiases(biases)

  val nextInputsOpcode = 1.toByte
  val nextTransmittingOpcode = 2.toByte
  val nextCalculatingOpcode = 3.toByte
  val incrementAddressOpcode = 4.toByte

  "Should initially set address to 0, then increment to 1 after one increment message via UART." in {
    test(new Top(8, dimension, frequency, baudRate, mappedInputs, mappedWeights,mappedBiases, signs, fixedPoints)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      dut.io.address.get.expect(0.U)

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

      dut.io.address.get.expect(1.U)
    }
  }

  "Should initially set address to 0, then increment to 3 after three increment messages via UART." in {
    test(new Top(8, dimension, frequency, baudRate, mappedInputs, mappedWeights,mappedBiases, signs, fixedPoints)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      dut.io.address.get.expect(0.U)

      dut.io.rxd.poke(1.U(1.W)) // UART idle signal is high
      dut.clock.step(100)

      val bytesToSend = Array(
        incrementAddressOpcode,
        incrementAddressOpcode,
        incrementAddressOpcode
      )

      val uartBitsToSend = Utils.UartCoding.encodeBytesToUartBits(bytesToSend)

      println("Sending bit vector: " + uartBitsToSend)

      uartBitsToSend.foreach(bit => {
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))
        dut.clock.step(cyclesPerSerialBit)
      })

      dut.io.address.get.expect(3.U)
    }
  }

  "Should read from memory" in {
    test(new Top(8, dimension, frequency, baudRate, mappedInputs, mappedWeights,mappedBiases, signs, fixedPoints)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      dut.io.address.get.expect(0.U)

      val memoryValues = Array.fill(dimension * dimension)(0)
      for (i <- 0 until dimension * dimension) {
        memoryValues(i) = dut.io.debugMatrixMemory1.get(i).peekInt().toInt
      }

      print(MatrixUtils.matrixToString(MatrixUtils.convertMappedMatrixToMatrix(memoryValues, dimension)))
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

      dut.io.address.get.expect(1.U)


      for (i <- 0 until dimension * dimension) {
        memoryValues(i) = dut.io.debugMatrixMemory1.get(i).peekInt().toInt
      }
      print(MatrixUtils.matrixToString(MatrixUtils.convertMappedMatrixToMatrix(memoryValues, dimension)))
    }
  }

  "Should write and then read from memory" in {
    test(new Top(8, dimension, frequency, baudRate, mappedInputs, mappedWeights,mappedBiases, signs, fixedPoints)) { dut =>

      println("Test: Should write and then read from memory")

      dut.clock.setTimeout(clockTimeout)

      dut.io.address.get.expect(0.U)

      val initialMemoryValues = Array.fill(dimension * dimension)(0)
      for (i <- 0 until dimension * dimension) {
        initialMemoryValues(i) = dut.io.debugMatrixMemory1.get(i).peekInt().toInt
      }

      println("Initial memory state:")
      print(MatrixUtils.matrixToString(MatrixUtils.convertMappedMatrixToMatrix(initialMemoryValues, dimension)))

      dut.io.rxd.poke(1.U(1.W)) // UART idle signal is high
      dut.clock.step(100)

      val bytesToSend = Array(
        nextInputsOpcode
      )

      val uartBitsToSend = Utils.UartCoding.encodeBytesToUartBits(bytesToSend)

      println("Sending bit vector (nextInputsOpcode): " + uartBitsToSend)

      uartBitsToSend.foreach(bit => {
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))
        dut.clock.step(cyclesPerSerialBit)
      })

      // Let it settle
      dut.clock.step(100)

      val memoryStateAfterOpcode = Array.fill(dimension * dimension)(0)
      for (i <- 0 until dimension * dimension) {
        memoryStateAfterOpcode(i) = dut.io.debugMatrixMemory1.get(i).peekInt().toInt
      }

      println("Memory state after opcode (should match initial):")
      print(MatrixUtils.matrixToString(MatrixUtils.convertMappedMatrixToMatrix(memoryStateAfterOpcode, dimension)))
      assert(memoryStateAfterOpcode sameElements initialMemoryValues)

      val newMemoryBytesToSend = Array(
        32.toByte, 2.toByte, 3.toByte,
        4.toByte, 5.toByte, 6.toByte,
        7.toByte, 8.toByte, 21.toByte
      )

      val newMemoryBitsToSend = Utils.UartCoding.encodeBytesToUartBits(newMemoryBytesToSend)
      println("Sending memory bit vector: " + newMemoryBitsToSend)

      newMemoryBitsToSend.foreach(bit => {
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))
        dut.clock.step(cyclesPerSerialBit)
      })

      val memoryStateAfterData = Array.fill(dimension * dimension)(0)
      dut.clock.step(100)
      for (i <- 0 until dimension * dimension) {
        memoryStateAfterData(i) = dut.io.debugMatrixMemory1.get(i).peekInt().toInt
      }

      println("Current memory state (should match newMemoryBytesToSend):")
      print(MatrixUtils.matrixToString(MatrixUtils.convertMappedMatrixToMatrix(memoryStateAfterData, dimension)))
      assert(memoryStateAfterData sameElements newMemoryBytesToSend)
      println(dut.io.states.peek())
      dut.io.address.get.expect(0.U)
    }
  }

  "Should transmit memory contents." in {
    test(new Top(8, dimension, frequency, baudRate, mappedInputs, mappedWeights,mappedBiases, signs, fixedPoints)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      dut.io.rxd.poke(1.U(1.W)) // UART idle signal is high
      dut.clock.step(100)

      val bytesToSend = Array(
        nextTransmittingOpcode
      )

      val uartBitsToSend = Utils.UartCoding.encodeBytesToUartBits(bytesToSend)

      println("Sending bit vector: " + uartBitsToSend)
      var c = 0
      val uartOutput = ListBuffer[BigInt]()
      uartBitsToSend.foreach(bit => {
        uartOutput.append(dut.io.txd.peekInt())
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))
        dut.clock.step(cyclesPerSerialBit)
      })

      for (j <- 0 until dimension * dimension) {

        for (i <- 0 until 11) {
          uartOutput.append(dut.io.txd.peekInt())
          dut.clock.step(cyclesPerSerialBit)
        }
        //print("Message " + j + ": ")

        //val bytesFromEmittedUartFrames = Utils.UartCoding.decodeUartBitsToByteArray(uartOutput.toArray)
        //
        //println("Bytes from emitted UART frames: " + bytesFromEmittedUartFrames.mkString(", "))
      }
      println("uartOutput: " + uartOutput.mkString)
      val bytesFromEmittedUartFrames = Utils.UartCoding.decodeUartBitsToByteArray(uartOutput.toArray)
      println("Bytes from emitted UART frames: " + bytesFromEmittedUartFrames.mkString(", "))

    }
  }

}
