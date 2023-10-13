import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class BufferSpec extends AnyFreeSpec with ChiselScalatestTester {
  "Buffer should behave correctly" in {
    val dimension = 4
    val shift = 0
    test(new Buffer(w = 8, dimension = dimension, shift = shift)) { dut =>
      dut.io.load.poke(true.B)
      dut.io.data(0).poke(1.U)
      dut.io.data(1).poke(2.U)
      dut.io.data(2).poke(3.U)
      dut.io.data(3).poke(4.U)
      dut.io.output.expect(0)

      for (i <- 0 until shift) {
        dut.clock.step(1)
        dut.io.load.poke(false.B)
        dut.io.output.expect(0)
      }

      dut.clock.step(1)
      dut.io.load.poke(false.B)
      dut.io.output.expect(1)

      dut.clock.step(1)
      dut.io.output.expect(2)

      dut.clock.step(1)
      dut.io.output.expect(3)

      dut.clock.step(1)
      dut.io.output.expect(4)

      for (i <- 0 until dimension) {
        dut.clock.step(1)
        dut.io.output.expect(0)
      }
    }
  }
}
