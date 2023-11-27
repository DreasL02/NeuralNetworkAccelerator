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

  val inputsL1: Array[Array[Float]] = Array(Array(1.0f, 2.3f, 3.0f), Array(4.0f, 5.05f, 6.0f), Array(7.0f, 8.6f, 9.0f))
  val fixedPointL1: Int = 4

  val inputsL2: Array[Array[Float]] = Array(Array(0f, 0.1f, 0.2f), Array(3.5f, 5.6f, 6.2f), Array(8.1f, 8.3f, 9.2f))
  val fixedPointL2: Int = 4

  val inputs: Array[Array[Array[Byte]]] = Array(
    Configuration.convertFloatMatrixToFixedMatrixBytes(inputsL1, fixedPointL1),
    Configuration.convertFloatMatrixToFixedMatrixBytes(inputsL2, fixedPointL2)
  )
  var mappedInputs = Configuration.mapInputs(inputs)

  val nextInputsOpcode = 1.toByte
  val nextTransmittingOpcode = 2.toByte
  val nextCalculatingOpcode = 3.toByte
  val incrementAddressOpcode = 4.toByte

  "Should initially set address to 0, then increment to 1 after one increment message via UART." in {
    test(new Accelerator(8, dimension, frequency, baudRate, mappedInputs)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      dut.io.address.expect(0.U)

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
    }
  }

  "Should initially set address to 0, then increment to 3 after three increment messages via UART." in {
    test(new Accelerator(8, dimension, frequency, baudRate, mappedInputs)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      dut.io.address.expect(0.U)

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

      dut.io.address.expect(3.U)
    }
  }

  "Should read from memory" in {
    test(new Accelerator(8, dimension, frequency, baudRate, mappedInputs)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      dut.io.address.expect(0.U)

      val memoryValues = Array.fill(dimension * dimension)(0)
      for (i <- 0 until dimension * dimension) {
        memoryValues(i) = dut.io.matrixMemory(i).peekInt().toInt
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

      dut.io.address.expect(1.U)


      for (i <- 0 until dimension * dimension) {
        memoryValues(i) = dut.io.matrixMemory(i).peekInt().toInt
      }
      print(MatrixUtils.matrixToString(MatrixUtils.convertMappedMatrixToMatrix(memoryValues, dimension)))
    }
  }

  "Should write and then read from memory" in {
    test(new Accelerator(8, dimension, frequency, baudRate, mappedInputs)) { dut =>

      println("Test: Should write and then read from memory")

      dut.clock.setTimeout(clockTimeout)

      dut.io.address.expect(0.U)

      val initialMemoryValues = Array.fill(dimension * dimension)(0)
      for (i <- 0 until dimension * dimension) {
        initialMemoryValues(i) = dut.io.matrixMemory(i).peekInt().toInt
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
        memoryStateAfterOpcode(i) = dut.io.matrixMemory(i).peekInt().toInt
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
        memoryStateAfterData(i) = dut.io.matrixMemory(i).peekInt().toInt
      }

      println("Current memory state (should match newMemoryBytesToSend):")
      print(MatrixUtils.matrixToString(MatrixUtils.convertMappedMatrixToMatrix(memoryStateAfterData, dimension)))
      assert(memoryStateAfterData sameElements newMemoryBytesToSend)
      dut.io.address.expect(0.U)
    }
  }

  "Should transmit memory contents." in {
    test(new Accelerator(8, dimension, frequency, baudRate, mappedInputs)) { dut =>

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

      for (j <- 0 until dimension * dimension + 30) {

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
