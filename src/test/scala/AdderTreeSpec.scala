import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class AdderTreeSpec extends AnyFreeSpec with ChiselScalatestTester {
  "AdderTree should sum all the values in the input channel" in {
    test(new AdderTree(w = 8, numberOfInputs = 4)) { dut =>
      dut.io.inputChannel.valid.poke(true.B)
      dut.io.inputChannel.bits(0).poke(1.U)
      dut.io.inputChannel.bits(1).poke(2.U)
      dut.io.inputChannel.bits(2).poke(3.U)
      dut.io.inputChannel.bits(3).poke(4.U)
      dut.io.resultChannel.ready.poke(true.B)
      dut.io.resultChannel.valid.expect(false.B)
      dut.io.resultChannel.bits.expect(0.U)

      while (!dut.io.resultChannel.valid.peek().litToBoolean) {
        dut.clock.step()
      }
      dut.io.resultChannel.valid.expect(true.B)
      dut.io.resultChannel.bits.expect(10.U)
    }
  }

  "AdderTree should sum all the values in the input channel with 3 inputs" in {
    test(new AdderTree(w = 8, numberOfInputs = 3)) { dut =>
      dut.io.inputChannel.valid.poke(true.B)
      dut.io.inputChannel.bits(0).poke(1.U)
      dut.io.inputChannel.bits(1).poke(2.U)
      dut.io.inputChannel.bits(2).poke(3.U)
      dut.io.resultChannel.ready.poke(true.B)
      dut.io.resultChannel.valid.expect(false.B)
      dut.io.resultChannel.bits.expect(0.U)

      while (!dut.io.resultChannel.valid.peek().litToBoolean) {
        dut.clock.step()
      }

      dut.io.resultChannel.valid.expect(true.B)
      dut.io.resultChannel.bits.expect(6.U)
    }
  }


}
