import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import communication.chisel.lib.uart.UartRx

import scala.collection.mutable.ListBuffer

class UartRxSpec extends AnyFreeSpec with ChiselScalatestTester {

  val clockTimeout = 200_000_000
  val frequency = 100
  val baudRate = 1
  val cyclesPerSerialBit = utils.UartCoding.cyclesPerSerialBit(frequency, baudRate)
  val tenSeconds = frequency * 10

  "UartRx should behave correctly" in {
    test(new UartRx(frequency, baudRate)) { dut =>

      val testValue = 113.toByte

      val bitsToSend = utils.UartCoding.encodeBytesToUartBits(Array(testValue))
      println("Sending bit vector: " + bitsToSend)

      dut.io.rxd.poke(1.U(1.W)) // UART idle signal is high
      dut.io.outputChannel.ready.poke(false.B)
      dut.io.outputChannel.valid.expect(false.B)

      dut.clock.step(10)

      bitsToSend.foreach { bit =>
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))
        dut.clock.step(cyclesPerSerialBit)
      }

      while (!dut.io.outputChannel.valid.peek().litToBoolean) {
        dut.clock.step()
      }

      dut.io.outputChannel.bits.expect(testValue.U)
    }
  }

  "Output should initially be invalid" in {
    test(new UartRx(frequency, baudRate)) { dut =>
      dut.io.rxd.poke(1.U(1.W)) // UART idle signal is high
      for (i <- 0 until tenSeconds) {
        dut.io.outputChannel.valid.expect(false.B)
        dut.clock.step()
      }
    }
  }

  "Timing should be correct" in {
    test(new UartRx(frequency, baudRate)) { dut =>

      val low = 0.U(1.W)
      val high = 1.U(1.W)

      dut.io.outputChannel.valid.expect(false.B)

      dut.io.rxd.poke(high)
      dut.clock.step(tenSeconds)

      dut.io.outputChannel.valid.expect(false.B)

      for (i <- 0 until 8) {
        dut.io.rxd.poke(low)
        dut.clock.step(cyclesPerSerialBit)
        dut.io.outputChannel.valid.expect(false.B)
      }

      dut.clock.step(cyclesPerSerialBit)
      dut.io.outputChannel.valid.expect(true.B)
      dut.io.outputChannel.bits.expect(0.U(8.W))

      dut.io.rxd.poke(high) // Back to idle
      dut.clock.step(tenSeconds)
      dut.io.outputChannel.valid.expect(true.B)
    }
  }
}
