import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import communication.chisel.lib.uart.SerializingByteBuffer

class SerializingByteBufferSpec extends AnyFreeSpec with ChiselScalatestTester {

  "Output should be invalid by default" in {
    test(new SerializingByteBuffer(1)) { dut =>
      dut.io.outputChannel.valid.expect(false.B)
    }
  }

  "Input should be ready by default" in {
    test(new SerializingByteBuffer(1)) { dut =>
      dut.io.inputChannel.ready.expect(true.B)
    }
  }

  "It should serialize one byte" in {
    test(new SerializingByteBuffer(1)) { dut =>

      val testValue = 187.U(8.W)

      dut.io.inputChannel.bits(0).poke(testValue)
      dut.io.inputChannel.valid.poke(true.B)
      dut.io.outputChannel.ready.poke(true.B)

      dut.clock.step()

      dut.io.outputChannel.valid.expect(true.B)
      dut.io.inputChannel.ready.expect(false.B)
      dut.io.outputChannel.bits.expect(testValue)
    }
  }

  "It should serialize two bytes" in {
    test(new SerializingByteBuffer(2)) { dut =>

      val testValue1 = 187.U(8.W)
      val testValue2 = 109.U(8.W)

      // Load both bytes into the buffer at once.
      dut.io.inputChannel.bits(0).poke(testValue1)
      dut.io.inputChannel.bits(1).poke(testValue2)
      dut.io.inputChannel.valid.poke(true.B)
      dut.io.outputChannel.ready.poke(true.B)

      dut.clock.step()

      // Now the first byte should be outputted.
      dut.io.outputChannel.valid.expect(true.B)
      dut.io.inputChannel.ready.expect(false.B)
      dut.io.outputChannel.bits.expect(testValue1)

      dut.clock.step()

      // Now the second byte should be outputted.
      dut.io.outputChannel.valid.expect(true.B)
      dut.io.inputChannel.ready.expect(false.B)
      dut.io.outputChannel.bits.expect(testValue2)

      dut.clock.step()

      // Now the output should be invalid and the input ready to receive a new buffer.
      dut.io.outputChannel.valid.expect(false.B)
      dut.io.inputChannel.ready.expect(true.B)
    }
  }

  "It should hold the first byte until receiver is ready" in {
    test(new SerializingByteBuffer(2)) { dut =>

      val testValue1 = 187.U(8.W)
      val testValue2 = 109.U(8.W)

      // Load both bytes into the buffer at once.
      dut.io.inputChannel.bits(0).poke(testValue1)
      dut.io.inputChannel.bits(1).poke(testValue2)
      dut.io.inputChannel.valid.poke(true.B)
      dut.io.outputChannel.ready.poke(false.B)

      dut.clock.step()

      // Now the first byte should be outputted.
      dut.io.outputChannel.valid.expect(true.B)
      dut.io.inputChannel.ready.expect(false.B)
      dut.io.outputChannel.bits.expect(testValue1)

      dut.clock.step()

      // The first byte should still be outputted since the receiver is not ready.
      dut.io.outputChannel.valid.expect(true.B)
      dut.io.inputChannel.ready.expect(false.B)
      dut.io.outputChannel.bits.expect(testValue1)

      dut.io.outputChannel.ready.poke(true.B)
      dut.clock.step()

      dut.io.outputChannel.valid.expect(true.B)
      dut.io.outputChannel.bits.expect(testValue2)
    }
  }


  "It should serialize two bytes and hold values until receiver is ready" in {
    test(new SerializingByteBuffer(2)) { dut =>

      val testValue1 = 187.U(8.W)
      val testValue2 = 109.U(8.W)

      // Load both bytes into the buffer at once.
      dut.io.inputChannel.bits(0).poke(testValue1)
      dut.io.inputChannel.bits(1).poke(testValue2)
      dut.io.inputChannel.valid.poke(true.B)

      dut.io.outputChannel.ready.poke(true.B)
      dut.clock.step()

      // Now the first byte should be outputted.
      dut.io.outputChannel.valid.expect(true.B)
      dut.io.inputChannel.ready.expect(false.B)
      dut.io.outputChannel.bits.expect(testValue1)

      dut.io.outputChannel.ready.poke(false.B)
      dut.clock.step()

      // The first byte should still be outputted since the receiver is not ready.
      dut.io.outputChannel.valid.expect(true.B)
      dut.io.inputChannel.ready.expect(false.B)
      dut.io.outputChannel.bits.expect(testValue1)

      dut.clock.step(100)

      // The first byte should still be outputted since the receiver is not ready.
      dut.io.outputChannel.valid.expect(true.B)
      dut.io.inputChannel.ready.expect(false.B)
      dut.io.outputChannel.bits.expect(testValue1)

      dut.io.outputChannel.ready.poke(true.B)
      dut.clock.step()

      // Now the second byte should be outputted since the receiver is ready.
      dut.io.outputChannel.valid.expect(true.B)
      dut.io.inputChannel.ready.expect(false.B)
      dut.io.outputChannel.bits.expect(testValue2)

      dut.clock.step()

      // Now the output should be invalid and the input ready to receive a new buffer.
      dut.io.outputChannel.valid.expect(false.B)
      dut.io.inputChannel.ready.expect(true.B)
    }
  }


  "It should serialize 10 bytes" in {
    test(new SerializingByteBuffer(10)) { dut =>

      val testValues = Array(
        187.U(8.W),
        109.U(8.W),
        108.U(8.W),
        134.U(8.W),
        0.U(8.W),
        1.U(8.W),
        100.U(8.W),
        127.U(8.W),
        2.U(8.W),
        3.U(8.W),
        99.U(8.W),
      )

      // Load all bytes into the buffer at once.
      for (i <- 0 until 10) {
        dut.io.inputChannel.bits(i).poke(testValues(i))
      }
      dut.io.inputChannel.valid.poke(true.B)
      dut.io.outputChannel.ready.poke(false.B)

      dut.clock.step()

      dut.io.outputChannel.bits.expect(testValues(0))

      for (i <- 1 until 10) {
        dut.io.outputChannel.valid.expect(true.B)
        dut.io.outputChannel.ready.poke(false.B)
        dut.clock.step(10)

        dut.io.outputChannel.valid.expect(true.B)
        dut.io.outputChannel.ready.poke(true.B)

        dut.clock.step()

        dut.io.outputChannel.valid.expect(true.B)
        dut.io.outputChannel.bits.expect(testValues(i))
      }

      dut.clock.step()
      dut.io.outputChannel.valid.expect(false.B)
      dut.io.inputChannel.ready.expect(true.B)

    }
  }


}
