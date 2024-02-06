import chisel3._
import chiseltest._
import communication.chisel.lib.uart.BufferedUartTxForTestingOnly
import org.scalatest.freespec.AnyFreeSpec

import scala.collection.mutable.ListBuffer

class BufferedUartTxSpec extends AnyFreeSpec with ChiselScalatestTester {

  val clockTimeout = 200_000_000
  val frequency = 5000 * 2
  val baudRate = 10
  val cyclesPerSerialBit = utils.UartCoding.cyclesPerSerialBit(frequency, baudRate)
  val tenSeconds = frequency * 10
  val uartFrameSize = 11

  val high = 1.U(1.W)
  val low = 0.U(1.W)

  "Should send UART idle signal by default" in {
    test(new BufferedUartTxForTestingOnly(frequency, baudRate, 1)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      for (i <- 0 until tenSeconds) {
        dut.io.inputChannel.ready.expect(true.B)
        dut.io.txd.expect(1.U(1.W))
        dut.clock.step(1)
      }
    }
  }

  "Should support a single byte buffer" in {
    test(new BufferedUartTxForTestingOnly(frequency, baudRate, 1)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      dut.io.inputChannel.ready.expect(true.B)

      val testValue = 117.toByte
      dut.io.inputChannel.bits(0).poke(testValue.U(8.W))
      dut.io.inputChannel.valid.poke(true.B)

      val uartOutput = ListBuffer[BigInt]()
      for (i <- 0 until uartFrameSize) {
        uartOutput.append(dut.io.txd.peekInt())
        dut.clock.step(cyclesPerSerialBit)
      }

      println("uartOutput: " + uartOutput.mkString)

      val bytesFromEmittedUartFrames = utils.UartCoding.decodeUartBitsToByteArray(uartOutput.toArray)
      println("Bytes from emitted UART frames: " + bytesFromEmittedUartFrames.mkString(", "))
      assert(bytesFromEmittedUartFrames.length == 1)
      assert(bytesFromEmittedUartFrames(0) == testValue)
    }
  }

  "Should support a two byte buffer" in {
    test(new BufferedUartTxForTestingOnly(frequency, baudRate, 2)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      dut.io.inputChannel.ready.expect(true.B)

      val testValue1 = 117.toByte
      val testValue2 = 73.toByte
      dut.io.inputChannel.bits(0).poke(testValue1.U(8.W))
      dut.io.inputChannel.bits(1).poke(testValue2.U(8.W))
      dut.io.inputChannel.valid.poke(true.B)
      dut.clock.step()
      dut.io.inputChannel.bits(0).poke(0.U(8.W))
      dut.io.inputChannel.bits(1).poke(0.U(8.W))
      dut.io.inputChannel.valid.poke(false.B)

      val uartOutput = ListBuffer[BigInt]()
      for (i <- 0 until 2 * uartFrameSize) {
        uartOutput.append(dut.io.txd.peekInt())
        dut.clock.step(cyclesPerSerialBit)
      }

      println("uartOutput: " + uartOutput.mkString)

      val bytesFromEmittedUartFrames = utils.UartCoding.decodeUartBitsToByteArray(uartOutput.toArray)
      println("Bytes from emitted UART frames: " + bytesFromEmittedUartFrames.mkString(", "))
      assert(bytesFromEmittedUartFrames.length == 2)
      assert(bytesFromEmittedUartFrames(0) == testValue1)
      assert(bytesFromEmittedUartFrames(1) == testValue2)

      dut.clock.step(cyclesPerSerialBit)

      // Check that the output remains idle
      for (i <- 0 until tenSeconds) {
        dut.io.txd.expect(high)
        dut.clock.step()
      }
    }
  }
  /*
    "Should support a nine byte buffer" in {
      test(new BufferedUartTxForTestingOnly(frequency, baudRate, 10)) { dut =>
        println("THIS TEST --- ----- ")
        dut.clock.setTimeout(clockTimeout)

        dut.io.inputChannel.ready.expect(true.B)

        val testValue1 = 48
        val testValue2 = 37
        val testValue3 = 16
        val testValue4 = 96
        val testValue5 = 81
        val testValue6 = 64
        val testValue7 = 144
        val testValue8 = 138
        val testValue9 = 112
        val testValue10 = 0

        dut.io.inputChannel.bits(0).poke(testValue1.U(8.W))
        dut.io.inputChannel.bits(1).poke(testValue2.U(8.W))
        dut.io.inputChannel.bits(2).poke(testValue3.U(8.W))
        dut.io.inputChannel.bits(3).poke(testValue4.U(8.W))
        dut.io.inputChannel.bits(4).poke(testValue5.U(8.W))
        dut.io.inputChannel.bits(5).poke(testValue6.U(8.W))
        dut.io.inputChannel.bits(6).poke(testValue7.U(8.W))
        dut.io.inputChannel.bits(7).poke(testValue8.U(8.W))
        dut.io.inputChannel.bits(9).poke(testValue9.U(8.W))
        dut.io.inputChannel.bits(8).poke(testValue10.U(8.W))

        dut.io.inputChannel.valid.poke(true.B)
        dut.clock.step()

        var bufferContents = ListBuffer[BigInt]()
        for (i <- 0 until 10) {
          bufferContents.append(dut.io.debug(i).peekInt())
        }
        print("counter contents: " + dut.io.debugCounter.peekInt() + " ")
        print("input contents: " + dut.io.outputBuffer.peekInt() + " ")
        println("buffer contents: " + bufferContents.mkString(", "))

        //dut.io.inputChannel.bits(0).poke(0.U(8.W))
        //dut.io.inputChannel.bits(1).poke(0.U(8.W))


        //dut.io.inputChannel.valid.poke(false.B)

        val uartOutput = ListBuffer[BigInt]()

        for (i <- 0 until 15 * uartFrameSize) {
          uartOutput.append(dut.io.txd.peekInt())
          dut.clock.step(cyclesPerSerialBit)
          bufferContents = ListBuffer[BigInt]()
          for (i <- 0 until 10) {
            bufferContents.append(dut.io.debug(i).peekInt())
          }
          print("counter contents: " + dut.io.debugCounter.peekInt() + " ")
          print("input contents: " + dut.io.outputBuffer.peekInt() + " ")
          println("buffer contents: " + bufferContents.mkString(", "))
        }

        println("uartOutput: " + uartOutput.mkString)

        val bytesFromEmittedUartFrames = Utils.UartCoding.decodeUartBitsToByteArray(uartOutput.toArray)
        println("Bytes from emitted UART frames: " + bytesFromEmittedUartFrames.mkString(", "))
        //assert(bytesFromEmittedUartFrames.length == 2)
        //assert(bytesFromEmittedUartFrames(0) == testValue1)
        //assert(bytesFromEmittedUartFrames(1) == testValue2)

        dut.clock.step(cyclesPerSerialBit)

        // Check that the output remains idle
        for (i <- 0 until tenSeconds) {
          //dut.io.txd.expect(high)
          dut.clock.step()
        }
        println("END--- ----- ")

      }
    }

  */
  "Should support refilling a two byte buffer" in {
    test(new BufferedUartTxForTestingOnly(frequency, baudRate, 2)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      dut.io.inputChannel.ready.expect(true.B)

      val testValue1 = 117.toByte
      val testValue2 = 73.toByte
      dut.io.inputChannel.bits(0).poke(testValue1.U(8.W))
      dut.io.inputChannel.bits(1).poke(testValue2.U(8.W))
      dut.io.inputChannel.valid.poke(true.B)
      dut.clock.step()
      dut.io.inputChannel.bits(0).poke(0.U(8.W))
      dut.io.inputChannel.bits(1).poke(0.U(8.W))
      dut.io.inputChannel.valid.poke(false.B)

      val uartOutput1 = ListBuffer[BigInt]()
      for (i <- 0 until 2 * uartFrameSize) {
        uartOutput1.append(dut.io.txd.peekInt())
        dut.clock.step(cyclesPerSerialBit)
      }

      println("uartOutput1: " + uartOutput1.mkString)

      val bytesFromEmittedUartFrames1 = utils.UartCoding.decodeUartBitsToByteArray(uartOutput1.toArray)
      println("Bytes from emitted UART frames 1: " + bytesFromEmittedUartFrames1.mkString(", "))
      assert(bytesFromEmittedUartFrames1.length == 2)
      assert(bytesFromEmittedUartFrames1(0) == testValue1)
      assert(bytesFromEmittedUartFrames1(1) == testValue2)

      dut.clock.step(cyclesPerSerialBit)

      for (i <- 0 until tenSeconds) {
        dut.io.txd.expect(high)
        dut.clock.step()
      }

      // -----------------------------
      // Refill the buffer
      // -----------------------------

      dut.clock.step(1)

      // Sanity check
      dut.io.inputChannel.ready.expect(true.B)

      val testValue3 = 33.toByte
      val testValue4 = 44.toByte
      dut.io.inputChannel.bits(0).poke(testValue3.U(8.W))
      dut.io.inputChannel.bits(1).poke(testValue4.U(8.W))
      dut.io.inputChannel.valid.poke(true.B)

      val uartOutput2 = ListBuffer[BigInt]()
      for (i <- 0 until 2 * uartFrameSize) {
        uartOutput2.append(dut.io.txd.peekInt())
        dut.clock.step(cyclesPerSerialBit)
      }

      println("uartOutput2: " + uartOutput2.mkString)

      val bytesFromEmittedUartFrames2 = utils.UartCoding.decodeUartBitsToByteArray(uartOutput2.toArray)
      println("Bytes from emitted UART frames 2: " + bytesFromEmittedUartFrames2.mkString(", "))
      assert(bytesFromEmittedUartFrames2.length == 2)
      assert(bytesFromEmittedUartFrames2(0) == testValue3)
      assert(bytesFromEmittedUartFrames2(1) == testValue4)
    }
  }
}
