import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import communication.chisel.lib.uart.{BufferedUartRx}

class BufferedUartRxSpec extends AnyFreeSpec with ChiselScalatestTester {

  val clockTimeout = 200_000_000
  val frequency = 100
  val baudRate = 1
  val cyclesPerSerialBit = Utils.UartCoding.cyclesPerSerialBit(frequency, baudRate)
  val tenSeconds = frequency * 10

  "Should support a single byte buffer" in {
    test(new BufferedUartRx(frequency, baudRate)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      val testValue = 113.toByte

      val bitsToSend = Utils.UartCoding.encodeBytesToUartBits(Array(testValue))
      println("Sending bit vector: " + bitsToSend)

      dut.io.channel.ready.poke(true.B)

      bitsToSend.foreach { bit =>
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))

        var i = 0
        while (i < cyclesPerSerialBit & !dut.io.channel.valid.peek().litToBoolean) {
          i = i + 1
          dut.clock.step(1)
        }
      }

      println("Sent bit vector")

      while (!dut.io.channel.valid.peek().litToBoolean) {
        dut.clock.step(1)
      }

      dut.io.channel.bits(0).expect(testValue.U)
    }
  }

  "Should support a two byte buffer" in {
    test(new BufferedUartRx(frequency, baudRate, 2)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      val testValue1 = 113.toByte
      val testValue2 = 114.toByte

      val bitsToSend = Utils.UartCoding.encodeBytesToUartBits(Array(testValue1, testValue2))
      println("Sending bit vector: " + bitsToSend)

      dut.io.rxd.poke(1.U(1.W)) // UART idle signal is high
      dut.io.channel.ready.poke(false.B)
      dut.clock.step(10)

      bitsToSend.foreach { bit =>
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))

        var i = 0
        while (i < cyclesPerSerialBit & !dut.io.channel.valid.peek().litToBoolean) {
          i = i + 1
          dut.clock.step(1)
        }
      }

      println("Sent bit vector")

      while (!dut.io.channel.valid.peek().litToBoolean) {
        dut.clock.step()
      }

      dut.io.channel.bits(0).expect(testValue1.U)
      dut.io.channel.bits(1).expect(testValue2.U)
    }
  }

  "Should support a nine byte buffer" in {
    test(new BufferedUartRx(frequency, baudRate, 9)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      val newMemoryBytesToSend = Array(
        32.toByte, 2.toByte, 3.toByte,
        4.toByte, 5.toByte, 6.toByte,
        7.toByte, 8.toByte, 21.toByte
      )

      val bitsToSend = Utils.UartCoding.encodeBytesToUartBits(newMemoryBytesToSend)
      println("Sending bit vector: " + bitsToSend)

      dut.io.channel.ready.poke(true.B)

      bitsToSend.foreach { bit =>
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))

        var i = 0
        while (i < cyclesPerSerialBit & !dut.io.channel.valid.peek().litToBoolean) {
          i = i + 1
          dut.clock.step(1)
        }
      }

      println("Sent bit vector")

      while (!dut.io.channel.valid.peek().litToBoolean) {
        dut.clock.step()
      }

      for (i <- 0 until 9) {
        dut.io.channel.bits(i).expect(newMemoryBytesToSend(i).U)
      }
    }
  }


  "Should support multiple buffer fills of one byte" in {
    test(new BufferedUartRx(frequency, baudRate, 1)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      dut.io.channel.ready.poke(true.B)

      val testValue1 = 113.toByte

      val bitsToSend = Utils.UartCoding.encodeBytesToUartBits(Array(testValue1))
      println("Sending bit vector: " + bitsToSend)

      bitsToSend.foreach { bit =>
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))

        var i = 0
        while (i < cyclesPerSerialBit & !dut.io.channel.valid.peek().litToBoolean) {
          i = i + 1
          dut.clock.step(1)
        }
      }

      println("Sent bit vector")

      while (!dut.io.channel.valid.peek().litToBoolean) {
        dut.clock.step(1)
      }

      dut.io.channel.bits(0).expect(testValue1.U)


      // -------
      // First byte has now been read, so we can send the next one.
      // -------
      dut.clock.step(tenSeconds)

      val testValue2 = 114.toByte

      val bitsToSend2 = Utils.UartCoding.encodeBytesToUartBits(Array(testValue2))
      println("Sending bit vector: " + bitsToSend2)

      bitsToSend2.foreach { bit =>
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))

        var i = 0
        while (i < cyclesPerSerialBit & !dut.io.channel.valid.peek().litToBoolean) {
          i = i + 1
          dut.clock.step(1)
        }
      }

      println("Sent bit vector")

      while (!dut.io.channel.valid.peek().litToBoolean) {
        dut.clock.step(1)
      }

      dut.io.channel.bits(0).expect(testValue2.U)
    }
  }

  "Should support multiple buffer fills of two bytes" in {
    test(new BufferedUartRx(frequency, baudRate, 2)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      dut.io.channel.ready.poke(true.B)

      val testValue1 = 113.toByte
      val testValue2 = 114.toByte

      val bitsToSend = Utils.UartCoding.encodeBytesToUartBits(Array(testValue1, testValue2))
      println("Sending bit vector: " + bitsToSend)

      bitsToSend.foreach { bit =>
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))

        var i = 0
        while (i < cyclesPerSerialBit & !dut.io.channel.valid.peek().litToBoolean) {
          i = i + 1
          dut.clock.step(1)
        }
      }

      println("Sent bit vector")

      while (!dut.io.channel.valid.peek().litToBoolean) {
        dut.clock.step(1)
      }

      // -------
      // First two bytes has now been read, so we can send the next two.
      // -------
      dut.clock.step(tenSeconds)

      val testValue3 = 115.toByte
      val testValue4 = 116.toByte

      val bitsToSend2 = Utils.UartCoding.encodeBytesToUartBits(Array(testValue3, testValue4))
      println("Sending bit vector: " + bitsToSend2)

      bitsToSend2.foreach { bit =>
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))

        var i = 0
        while (i < cyclesPerSerialBit & !dut.io.channel.valid.peek().litToBoolean) {
          i = i + 1
          dut.clock.step(1)
        }
      }

      println("Sent bit vector")

      while (!dut.io.channel.valid.peek().litToBoolean) {
        dut.clock.step(1)
      }

      dut.io.channel.bits(0).expect(testValue3.U)
      dut.io.channel.bits(1).expect(testValue4.U)
    }
  }
}
