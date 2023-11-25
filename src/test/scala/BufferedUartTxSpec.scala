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

      val testValue = 113.toByte

      dut.io.inputChannel.ready.expect(true.B)
      dut.io.inputChannel.bits(0).poke(testValue.U(8.W))
      dut.io.inputChannel.valid.poke(true.B)

      val uartOutput = ListBuffer[BigInt]()

      dut.clock.step()

      while (!dut.io.inputChannel.ready.peek().litToBoolean) {
        uartOutput.append(dut.io.txd.peekInt())
        dut.clock.step(1)
      }

      println(uartOutput.toArray.mkString(""))
    }
  }
}
