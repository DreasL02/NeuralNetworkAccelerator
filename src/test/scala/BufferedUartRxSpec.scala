import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import communication.chisel.lib.uart.{BufferedUartRx}

import scala.collection.mutable.ListBuffer

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

      val bob = dut.io.channel.bits(0).peek().litValue
      println(bob)

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

      println("Sent bit vector")

      while (!dut.io.channel.valid.peek().litToBoolean) {
        dut.clock.step(1)
        // print(dut.io.uartRxValidDebug.peek().litToBoolean)
        // println(" " + dut.io.rxd.peek().litValue)
      }

      val bob1 = dut.io.channel.bits(0).peek().litValue
      val bob2 = dut.io.channel.bits(1).peek().litValue

      println(bob1)
      println(bob2)
    }
  }





}


// 0 10001110 10001110 11

// 0 10001110 11 | 0 10001110 11
