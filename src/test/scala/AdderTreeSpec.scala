import chisel3._
import chiseltest._
import operators.AdderTree
import org.scalatest.freespec.AnyFreeSpec

class AdderTreeSpec extends AnyFreeSpec with ChiselScalatestTester {
  val toPrint = true

  "AdderTree should sum all the values in the input channel" in {
    test(new AdderTree(w = 8, numberOfInputs = 4, toPrint = toPrint)) { dut =>
      dut.io.inputChannel.valid.poke(true.B)
      dut.io.inputChannel.bits(0).poke(1.U)
      dut.io.inputChannel.bits(1).poke(2.U)
      dut.io.inputChannel.bits(2).poke(3.U)
      dut.io.inputChannel.bits(3).poke(4.U)
      dut.io.outputChannel.ready.poke(true.B)
      dut.io.outputChannel.valid.expect(false.B)
      dut.io.outputChannel.bits.expect(0.U)


      var cycles = 0
      while (!dut.io.outputChannel.valid.peek().litToBoolean) {
        dut.clock.step()
        cycles += 1
        if (toPrint) {
          println("output = " + dut.io.outputChannel.bits.peek().litValue)
        }
      }
      if (toPrint) {
        println("Cycles: " + cycles)
      }
      dut.io.outputChannel.valid.expect(true.B)
      dut.io.outputChannel.bits.expect(10.U)
    }
  }

  "AdderTree should sum all the values in the input channel with 3 inputs" in {
    test(new AdderTree(w = 8, numberOfInputs = 3, toPrint = toPrint)) { dut =>
      dut.io.inputChannel.valid.poke(true.B)
      dut.io.inputChannel.bits(0).poke(1.U)
      dut.io.inputChannel.bits(1).poke(2.U)
      dut.io.inputChannel.bits(2).poke(3.U)
      dut.io.outputChannel.ready.poke(true.B)
      dut.io.outputChannel.valid.expect(false.B)
      dut.io.outputChannel.bits.expect(0.U)

      var cycles = 0
      while (!dut.io.outputChannel.valid.peek().litToBoolean) {
        dut.clock.step()
        cycles += 1
        if (toPrint) {
          println("output = " + dut.io.outputChannel.bits.peek().litValue)
        }
      }
      if (toPrint) {
        println("Cycles: " + cycles)
      }

      dut.io.outputChannel.valid.expect(true.B)
      dut.io.outputChannel.bits.expect(6.U)
    }
  }

  "AdderTree should sum all the values in the input channel with 10 inputs" in {
    test(new AdderTree(w = 8, numberOfInputs = 10, toPrint = toPrint)) { dut =>
      for (i <- 0 until 10) {
        dut.io.inputChannel.bits(i).poke(i.U)
      }
      dut.io.inputChannel.valid.poke(true.B)
      dut.io.outputChannel.ready.poke(true.B)
      dut.io.outputChannel.valid.expect(false.B)
      dut.io.outputChannel.bits.expect(0.U)

      if (toPrint) {
        println("output = " + dut.io.outputChannel.bits.peek().litValue)
      }

      var cycles = 0
      while (!dut.io.outputChannel.valid.peek().litToBoolean) {
        dut.clock.step()
        cycles += 1
        if (toPrint) {
          println("output = " + dut.io.outputChannel.bits.peek().litValue)
        }
      }
      if (toPrint) {
        println("Cycles: " + cycles)
      }
      dut.io.outputChannel.bits.expect(45.U)

      dut.io.inputChannel.valid.poke(false.B)
      dut.clock.step()
      dut.io.inputChannel.ready.expect(true.B)
    }
  }
}
