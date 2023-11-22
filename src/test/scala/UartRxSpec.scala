import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import communication.chisel.lib.uart.Rx
import org.scalatest.freespec.AnyFreeSpec

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
}

