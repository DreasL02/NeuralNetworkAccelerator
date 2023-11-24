import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class UartAcceleratorSpec extends AnyFreeSpec with ChiselScalatestTester {

  val clockTimeout = 200_000_000
  val frequency = 100
  val baudRate = 1
  val cyclesPerSerialBit = Utils.UartCoding.cyclesPerSerialBit(frequency, baudRate)

  val inputsL1: Array[Array[Float]] = Array(Array(1.0f, 2.3f, 3.0f), Array(4.0f, 5.05f, 6.0f), Array(7.0f, 8.6f, 9.0f))
  val fixedPointL1: Int = 4

  val inputsL2: Array[Array[Float]] = Array(Array(0f, 0.1f, 0.2f), Array(3.5f, 5.6f, 6.2f), Array(8.1f, 8.3f, 9.2f))
  val fixedPointL2: Int = 4

  val inputs: Array[Array[Array[Byte]]] = Array(
    Configuration.convertFloatMatrixToFixedMatrix(inputsL1, fixedPointL1),
    Configuration.convertFloatMatrixToFixedMatrix(inputsL2, fixedPointL2)
  )
  var mappedInputs = Configuration.mapInputs(inputs)

  val nextInputsOpcode = 1.toByte
  val incrementAddressOpcode = 4.toByte

  "Should initially set address to 0, then increment to 1 after one increment message via UART." in {
    test(new Accelerator(8, 3, frequency, baudRate, mappedInputs)) { dut =>

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
    test(new Accelerator(8, 3, frequency, baudRate, mappedInputs)) { dut =>

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
    test(new Accelerator(8, 3, frequency, baudRate, mappedInputs)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      dut.io.address.expect(0.U)
      dut.io.value.expect(48.U)

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
      dut.io.value.expect(3.U)
    }
  }

  "Should write and then read from memory" in {
    test(new Accelerator(8, 3, frequency, baudRate, mappedInputs)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      dut.io.address.expect(0.U)
      dut.io.value.expect(48.U)

      dut.io.rxd.poke(1.U(1.W)) // UART idle signal is high
      dut.clock.step(100)


      val bytesToSend = Array(
        nextInputsOpcode
      )

      val uartBitsToSend = Utils.UartCoding.encodeBytesToUartBits(bytesToSend)

      println("Sending bit vector: " + uartBitsToSend)

      uartBitsToSend.foreach(bit => {
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))
        dut.clock.step(cyclesPerSerialBit)
      })

      // Let it settle
      dut.clock.step(100)

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

      dut.clock.step(100)

      dut.io.address.expect(0.U)
      println(dut.io.value.peekInt())
    }
  }


}
