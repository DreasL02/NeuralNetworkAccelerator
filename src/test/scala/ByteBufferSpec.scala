import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import communication.chisel.lib.uart.{ByteBuffer}

class ByteBufferSpec extends AnyFreeSpec with ChiselScalatestTester {

  "Output should be invalid by default" in {
    test(new ByteBuffer(1)) { dut =>
      dut.io.outputChannel.valid.expect(false.B)
    }
  }

  "Should buffer a single byte" in {
    test(new ByteBuffer(1)) { dut =>

      val testValue = 187.U(8.W)

      dut.io.inputChannel.valid.poke(false.B)
      dut.clock.step()

      dut.io.outputChannel.valid.expect(false.B)

      dut.io.inputChannel.bits.poke(testValue)
      dut.io.inputChannel.valid.poke(true.B)

      dut.clock.step(2)

      dut.io.outputChannel.valid.expect(true.B)
      dut.io.outputChannel.bits(0).expect(testValue)
    }
  }

  "Should buffer two bytes" in {
    test(new ByteBuffer(2)) { dut =>

      val testValue1 = 187.U(8.W)
      val testValue2 = 109.U(8.W)

      dut.io.inputChannel.valid.poke(false.B)
      dut.clock.step()

      dut.io.outputChannel.valid.expect(false.B)

      // Insert first byte
      dut.io.inputChannel.bits.poke(testValue1)
      dut.io.inputChannel.valid.poke(true.B)
      dut.clock.step()

      // The output should still be invalid, only one byte has been inserted
      // The first byte should be buffered
      dut.io.outputChannel.valid.expect(false.B)
      dut.io.outputChannel.bits(0).expect(testValue1)

      dut.io.inputChannel.valid.poke(false.B)
      dut.clock.step()

      dut.io.outputChannel.valid.expect(false.B)
      dut.io.outputChannel.bits(0).expect(testValue1)

      // Insert second byte
      dut.io.inputChannel.bits.poke(testValue2)
      dut.io.inputChannel.valid.poke(true.B)

      dut.clock.step(2)

      println("dut.io.outputChannel.bits(0): " + dut.io.outputChannel.bits(0).peekInt())
      println("dut.io.outputChannel.bits(1): " + dut.io.outputChannel.bits(1).peekInt())
      println("dut.io.outputChannel.valid: " + dut.io.outputChannel.valid.peekBoolean())

      // The output should now be valid
      // Both bytes should be buffered
      dut.io.outputChannel.valid.expect(true.B)
      dut.io.outputChannel.bits(0).expect(testValue1)
      dut.io.outputChannel.bits(1).expect(testValue2)

      dut.clock.step(100)

      // The output should still be valid after a long time, since we have not signalled that we are ready to receive
      dut.io.outputChannel.valid.expect(true.B)
      dut.io.outputChannel.bits(0).expect(testValue1)
      dut.io.outputChannel.bits(1).expect(testValue2)
    }
  }

  "Should buffer two bytes, then overwrite with two new bytes" in {
    test(new ByteBuffer(2)) { dut =>

      val testValue1 = 187.U(8.W)
      val testValue2 = 109.U(8.W)

      dut.io.inputChannel.valid.poke(false.B)
      dut.clock.step()

      dut.io.outputChannel.valid.expect(false.B)

      dut.io.inputChannel.bits.poke(testValue1)
      dut.io.inputChannel.valid.poke(true.B)

      dut.clock.step()

      dut.io.outputChannel.valid.expect(false.B)
      dut.io.outputChannel.bits(0).expect(testValue1)

      dut.io.inputChannel.valid.poke(false.B)
      dut.clock.step()

      dut.io.outputChannel.valid.expect(false.B)
      dut.io.outputChannel.bits(0).expect(testValue1)

      dut.io.inputChannel.bits.poke(testValue2)
      dut.io.inputChannel.valid.poke(true.B)

      dut.clock.step(2)

      dut.io.outputChannel.valid.expect(true.B)
      dut.io.outputChannel.bits(0).expect(testValue1)
      dut.io.outputChannel.bits(1).expect(testValue2)

      // ------------------------------
      // Now overwrite the buffer
      // ------------------------------

      dut.io.inputChannel.valid.poke(false.B)
      dut.io.outputChannel.ready.poke(true.B)

      dut.clock.step()

      val testValue3 = 33.U(8.W)
      val testValue4 = 44.U(8.W)

      dut.io.inputChannel.bits.poke(testValue3)
      dut.io.inputChannel.valid.poke(true.B)

      dut.clock.step(5)

      dut.io.outputChannel.valid.expect(false.B)
      dut.io.outputChannel.bits(0).expect(testValue3)

      dut.io.inputChannel.valid.poke(false.B)
      dut.clock.step()

      dut.io.outputChannel.valid.expect(false.B)
      dut.io.outputChannel.bits(0).expect(testValue3)

      dut.io.inputChannel.bits.poke(testValue4)
      dut.io.inputChannel.valid.poke(true.B)

      dut.clock.step(2)

      dut.io.outputChannel.valid.expect(true.B)
      dut.io.outputChannel.bits(0).expect(testValue3)
      dut.io.outputChannel.bits(1).expect(testValue4)

    }
  }
}
