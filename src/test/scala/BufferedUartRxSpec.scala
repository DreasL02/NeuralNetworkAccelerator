import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import communication.chisel.lib.uart.{BufferedUartRx}

class BufferedUartRxSpec extends AnyFreeSpec with ChiselScalatestTester {

  "Should support a single byte buffer" in {
    test(new BufferedUartRx(100, 1)) { dut =>

      val testValue = 113.toByte

      val bitsToSend = Utils.UartCoding.encodeBytesToUartBits(Array(testValue))
      println("Sending bit vector: " + bitsToSend)

      dut.io.rxd.poke(1.U(1.W)) // UART idle signal is high
      dut.io.channel.ready.poke(false.B)
      dut.clock.step(10)

      bitsToSend.foreach { bit =>
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))
        dut.clock.step(99)
      }

      while (!dut.io.channel.valid.peek().litToBoolean) {
        dut.clock.step(1)
      }

      dut.io.channel.bits(0).expect(testValue.U)
    }
  }

  "Should support a two byte buffer" in {
    test(new BufferedUartRx(100, 1, 2)) { dut =>

      dut.clock.setTimeout(0)

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


        for (i <- 0 until 99) {
          dut.clock.step(1)
          if (false) {
            print("valid = " + dut.io.uartRxValidDebug.peek().litToBoolean)
            print(" bitsreg = " + dut.io.uartDebugBitsReg.peek().litValue)
            print(" cntreg = " + dut.io.uartDebugCntReg.peek().litValue)
            print(" rxd = " + dut.io.rxd.peek().litValue)
            print(" data = " + dut.io.channel.bits(0).peek().litValue)
            print(" i = " + i)
            print(" | buffercounter = " + dut.io.bufferCounter.peek().litValue)
            println()
          }

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


  "Should support multiple buffer fills of one byte" in {
    test(new BufferedUartRx(100, 1, 1)) { dut =>

      dut.io.rxd.poke(1.U(1.W)) // UART idle signal is high
      dut.io.channel.ready.poke(false.B)
      dut.clock.step(10)

      val testValue = 113.toByte

      val bitsToSend = Utils.UartCoding.encodeBytesToUartBits(Array(testValue))
      println("Sending bit vector: " + bitsToSend)

      bitsToSend.foreach { bit =>
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))
        dut.clock.step(99)
      }

      while (!dut.io.channel.valid.peek().litToBoolean) {
        dut.clock.step(1)
      }

      dut.io.channel.bits(0).expect(testValue.U)

      dut.io.channel.ready.poke(true.B)
      dut.clock.step()

      // -------
      // First byte has now been read, so we can send the next one.
      // -------

      dut.io.rxd.poke(1.U(1.W)) // UART idle signal is high
      dut.io.channel.ready.poke(false.B)
      dut.clock.step(10)


      val testValue2 = 114.toByte

      val bitsToSend2 = Utils.UartCoding.encodeBytesToUartBits(Array(testValue2))
      println("Sending bit vector: " + bitsToSend2)

      bitsToSend2.foreach { bit =>
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))
        dut.clock.step(99)
      }

      while (!dut.io.channel.valid.peek().litToBoolean) {
        dut.clock.step(1)
      }

      dut.io.channel.bits(0).expect(testValue2.U)
    }
  }

  "Should support multiple buffer fills of two bytes" in {
    test(new BufferedUartRx(100, 1, 2)) { dut =>

      dut.io.rxd.poke(1.U(1.W)) // UART idle signal is high
      dut.io.channel.ready.poke(false.B)
      dut.clock.step(10)

      val testValue1 = 113.toByte
      val testValue2 = 114.toByte

      val bitsToSend = Utils.UartCoding.encodeBytesToUartBits(Array(testValue1, testValue2))
      println("Sending bit vector: " + bitsToSend)

      bitsToSend.foreach { bit =>
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))
        dut.clock.step(99)
      }

      while (!dut.io.channel.valid.peek().litToBoolean) {
        dut.clock.step(1)
      }

      dut.io.channel.bits(0).expect(testValue1.U)
      dut.io.channel.bits(1).expect(testValue2.U)

      dut.io.channel.ready.poke(true.B)
      dut.clock.step()

      // -------
      // First two bytes has now been read, so we can send the next two.
      // -------

      dut.io.rxd.poke(1.U(1.W)) // UART idle signal is high
      dut.io.channel.ready.poke(false.B)
      dut.clock.step(10)

      val testValue3 = 115.toByte
      val testValue4 = 116.toByte

      val bitsToSend2 = Utils.UartCoding.encodeBytesToUartBits(Array(testValue3, testValue4))
      println("Sending bit vector: " + bitsToSend2)

      bitsToSend2.foreach { bit =>
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))
        dut.clock.step(99)
      }

      while (!dut.io.channel.valid.peek().litToBoolean) {
        dut.clock.step(1)
      }

      dut.io.channel.bits(0).expect(testValue3.U)
      dut.io.channel.bits(1).expect(testValue4.U)
    }
  }
}
