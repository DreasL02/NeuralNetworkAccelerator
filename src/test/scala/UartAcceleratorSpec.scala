import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class UartAcceleratorSpec extends AnyFreeSpec with ChiselScalatestTester {

  val clockTimeout = 200_000_000
  val frequency = 100
  val baudRate = 1
  val cyclesPerSerialBit = Utils.UartCoding.cyclesPerSerialBit(frequency, baudRate)

  "Should initally set address to 0, then increment to 1 after one increment message via UART." in {
    test(new Accelerator(8, 3, frequency, baudRate)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      dut.io.address.expect(0.U)

      dut.io.rxd.poke(1.U(1.W)) // UART idle signal is high
      dut.clock.step(100)

      val opcode = 4
      val bytesToSend = Array(
        opcode.toByte,
      )

      val uartBitsToSend = Utils.UartCoding.encodeBytesToUartBits(bytesToSend)

      println("Sending bit vector: " + uartBitsToSend)

      uartBitsToSend.foreach(bit => {
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))
        dut.clock.step(cyclesPerSerialBit)
      })

      dut.io.address.expect(1.U)
    }
  }

  "Should initally set address to 0, then increment to 3 after three increment messages via UART." in {
    test(new Accelerator(8, 3, frequency, baudRate)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      dut.io.address.expect(0.U)

      dut.io.rxd.poke(1.U(1.W)) // UART idle signal is high
      dut.clock.step(100)

      val opcode = 4
      val bytesToSend = Array(
        opcode.toByte,
        opcode.toByte,
        opcode.toByte
      )

      val uartBitsToSend = Utils.UartCoding.encodeBytesToUartBits(bytesToSend)

      println("Sending bit vector: " + uartBitsToSend)

      uartBitsToSend.foreach(bit => {
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.rxd.poke(bitAsBigInt.U(1.W))
        dut.clock.step(cyclesPerSerialBit)
      })

      dut.io.address.expect(3.U)
    }
  }




}
