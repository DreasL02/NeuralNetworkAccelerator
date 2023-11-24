import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import communication.chisel.lib.uart.{Rx}

import scala.collection.mutable.ListBuffer

/*

// TODO: Bytes are signed.
// TODO: Make the test test all values 0-255.
// TODO: Make the test test multiple bytes.
class UartRxSpec extends AnyFreeSpec with ChiselScalatestTester {

  "UartRx should behave correctly" in {
    test(new Rx(100, 1, 8)) { dut =>

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

      dut.io.channel.bits.expect(testValue.U)
    }
  }

  "UartRx should receive multiple bytes (3) correctly" in {
    test(new Rx(100, 1, 8 * 3)) { dut =>

      val testValue1 = 113.toByte
      val testValue2 =  97.toByte
      val testValue3 =  46.toByte

      val bitsToSend = Utils.UartCoding.encodeBytesToUartBits(Array(testValue1, testValue2, testValue3))
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

      val expected = (testValue3 << 16 | testValue2 << 8 | testValue1).U
      dut.io.channel.bits.expect(expected)
    }
  }

  "UartRx should receive multiple bytes (5) correctly" in {
    test(new Rx(100, 1, 8 * 3)) { dut =>

      val testValue1 = 108.toByte
      val testValue2 = 123.toByte
      val testValue3 = 1.toByte
      val testValue4 = 101.toByte
      val testValue5 = 3.toByte

      val bitsToSend = Utils.UartCoding.encodeBytesToUartBits(Array(testValue1, testValue2, testValue3))
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

      // val expected = (testValue5 << 32 | testValue4 << 24 | testValue3 << 16 | testValue2 << 8 | testValue1).U
      val expected = (testValue3 << 16 | testValue2 << 8 | testValue1).U


    }
  }


}


*/
