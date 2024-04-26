import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import communication.chisel.lib.uart.UartTx

import scala.collection.mutable.ListBuffer

class UartTxSpec extends AnyFreeSpec with ChiselScalatestTester {

  val clockTimeout = 200_000_000
  val frequency = 100
  val baudRate = 1
  val cyclesPerSerialBit = scala_utils.UartCoding.cyclesPerSerialBit(frequency, baudRate)
  val tenSeconds = frequency * 10

  val high = 1.U(1.W)
  val low = 0.U(1.W)

  "Output should be idle (high) by default" in {
    test(new UartTx(frequency, baudRate)) { dut =>
      dut.clock.setTimeout(clockTimeout)
      dut.io.rts.poke(true.B)
      dut.io.inputChannel.ready.expect(true.B)
      for (i <- 0 until tenSeconds) {
        dut.io.txd.expect(high)
        dut.clock.step()
      }
    }
  }

  "Timing should be correct" in {
    test(new UartTx(frequency, baudRate)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      val testValue = 107.toByte

      dut.io.rts.poke(true.B)
      dut.io.inputChannel.valid.poke(true.B)
      dut.io.inputChannel.bits.poke(testValue.U(8.W))

      val uartOutputBuffer = ListBuffer[BigInt]()

      for (i <- 0 until 11) {
        dut.clock.step(cyclesPerSerialBit)
        println(dut.io.inputChannel.ready.peekBoolean())
        uartOutputBuffer.append(dut.io.txd.peekInt())
      }

      val hardwareOutput = uartOutputBuffer.mkString("")
      val expectedOutput = scala_utils.UartCoding.encodeByteToUartBits(testValue)
      println(s"hardwareOutput: $hardwareOutput, expectedOutput: $expectedOutput")
      assert(hardwareOutput == expectedOutput)
    }
  }

  "Should respect RTS" in {
    test(new UartTx(frequency, baudRate)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      val testValue = 95.toByte

      // We put data on the input and mark it valid. We also set RTS to false, so the data should not be sent.
      dut.io.inputChannel.valid.poke(true.B)
      dut.io.inputChannel.bits.poke(testValue.U(8.W))
      dut.io.rts.poke(false.B)
      dut.io.inputChannel.ready.expect(false.B)

      for (_ <- 0 until tenSeconds) {
        dut.io.txd.expect(high)
        dut.clock.step()
      }

      dut.io.rts.poke(true.B)

      while (!dut.io.inputChannel.ready.peekBoolean()) {
        dut.clock.step()
      }

      val uartOutputBuffer = ListBuffer[BigInt]()

      for (i <- 0 until 11) {
        dut.clock.step(cyclesPerSerialBit)
        println(dut.io.inputChannel.ready.peekBoolean())
        uartOutputBuffer.append(dut.io.txd.peekInt())
      }

      val hardwareOutput = uartOutputBuffer.mkString("")
      val expectedOutput = scala_utils.UartCoding.encodeByteToUartBits(testValue)
      println(s"hardwareOutput: $hardwareOutput, expectedOutput: $expectedOutput")
      assert(hardwareOutput == expectedOutput)
    }
  }

}
