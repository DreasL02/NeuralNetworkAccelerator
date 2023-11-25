import chisel3._
import chiseltest._
import communication.chisel.lib.uart.BufferedUartTxForTestingOnly
import org.scalatest.freespec.AnyFreeSpec

import scala.collection.mutable.ListBuffer

class BufferedUartTxSpec extends AnyFreeSpec with ChiselScalatestTester {

  val clockTimeout = 200_000_000
  val frequency = 100
  val baudRate = 1
  val cyclesPerSerialBit = Utils.UartCoding.cyclesPerSerialBit(frequency, baudRate)
  val tenSeconds = frequency * 10
  val uartFrameSize = 11

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

      val bytesFromEmittedUartFrames = Utils.UartCoding.decodeUartBitsToByteArray(uartOutput.toArray)
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

      val uartOutput = ListBuffer[BigInt]()
      for (i <- 0 until 2*uartFrameSize) {
        uartOutput.append(dut.io.txd.peekInt())
        dut.clock.step(cyclesPerSerialBit)
      }

      println("uartOutput: " + uartOutput.mkString)

      val bytesFromEmittedUartFrames = Utils.UartCoding.decodeUartBitsToByteArray(uartOutput.toArray)
      println("Bytes from emitted UART frames: " + bytesFromEmittedUartFrames.mkString(", "))
      assert(bytesFromEmittedUartFrames.length == 2)
      assert(bytesFromEmittedUartFrames(0) == testValue1)
      assert(bytesFromEmittedUartFrames(1) == testValue2)
    }
  }

  "Should support refilling a two byte buffer" in {
    test(new BufferedUartTxForTestingOnly(frequency, baudRate, 2)) { dut =>

      // TODO: This test does not currently refill the buffer. It just sends two bytes.

      dut.clock.setTimeout(clockTimeout)

      dut.io.inputChannel.ready.expect(true.B)

      val testValue1 = 117.toByte
      val testValue2 = 73.toByte
      dut.io.inputChannel.bits(0).poke(testValue1.U(8.W))
      dut.io.inputChannel.bits(1).poke(testValue2.U(8.W))
      dut.io.inputChannel.valid.poke(true.B)

      val uartOutput = ListBuffer[BigInt]()
      for (i <- 0 until 2 * uartFrameSize) {
        uartOutput.append(dut.io.txd.peekInt())
        dut.clock.step(cyclesPerSerialBit)
      }

      println("uartOutput: " + uartOutput.mkString)

      val bytesFromEmittedUartFrames = Utils.UartCoding.decodeUartBitsToByteArray(uartOutput.toArray)
      println("Bytes from emitted UART frames: " + bytesFromEmittedUartFrames.mkString(", "))
      assert(bytesFromEmittedUartFrames.length == 2)
      assert(bytesFromEmittedUartFrames(0) == testValue1)
      assert(bytesFromEmittedUartFrames(1) == testValue2)
    }
  }


}
