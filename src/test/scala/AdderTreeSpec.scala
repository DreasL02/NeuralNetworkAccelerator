import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class AdderTreeSpec extends AnyFreeSpec with ChiselScalatestTester {
  "AdderTree should sum all the values in the input channel" in {
    test(new AdderTree(w = 8, numberOfValues = 4)) { c =>
      c.io.inputChannel.valid.poke(true.B)
      c.io.inputChannel.bits(0).poke(1.U)
      c.io.inputChannel.bits(1).poke(2.U)
      c.io.inputChannel.bits(2).poke(3.U)
      c.io.inputChannel.bits(3).poke(4.U)
      c.io.inputChannel.ready.expect(true.B)
      c.io.resultChannel.valid.expect(false.B)
      c.io.resultChannel.bits.expect(0.U)
      c.clock.step(1)
      c.io.resultChannel.valid.expect(false.B)
      c.io.resultChannel.bits.expect(0.U)
      c.clock.step(1)
      c.io.resultChannel.valid.expect(false.B)
      c.io.resultChannel.bits.expect(0.U)
      c.clock.step(1)
      c.io.resultChannel.valid.expect(true.B)
      c.io.resultChannel.bits.expect(10.U)
    }
  }

}
