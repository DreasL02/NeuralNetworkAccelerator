
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class InterfacerSpec extends AnyFreeSpec with ChiselScalatestTester {

  val cyclesToRun = 2
  "Interfacer should increment the input value by 1" in {
    test(new Interfacer(w = 8, cycles = cyclesToRun)) { dut =>
      var cycles = 0

      dut.io.inputChannel.bits.poke(3.U)
      dut.io.inputChannel.valid.poke(true.B)
      //dut.io.resultChannel.ready.poke(true.B)
      dut.io.resultChannel.valid.expect(false.B)
      dut.io.resultChannel.bits.expect(0.U)

      while (!dut.io.resultChannel.valid.peek().litToBoolean) {
        println("cycles: " + cycles)
        println("result bits: " + dut.io.resultChannel.bits.peek().litValue)
        println("start computation: " + dut.io.debugComputation.peek().litToBoolean)
        println("done with computation: " + dut.io.debugDone.peek().litToBoolean)

        println("in_ready: " + dut.io.inputChannel.ready.peek().litToBoolean)
        println("in_valid: " + dut.io.inputChannel.valid.peek().litToBoolean)
        println("res_ready: " + dut.io.resultChannel.ready.peek().litToBoolean)
        println("res_valid: " + dut.io.resultChannel.valid.peek().litToBoolean)
        dut.clock.step()

        cycles += 1
      }
      println("done")
      println("cycles: " + cycles)
      println("result bits: " + dut.io.resultChannel.bits.peek().litValue)
      println("start computation: " + dut.io.debugComputation.peek().litToBoolean)
      println("done with computation: " + dut.io.debugDone.peek().litToBoolean)

      println("in_ready: " + dut.io.inputChannel.ready.peek().litToBoolean)
      println("in_valid: " + dut.io.inputChannel.valid.peek().litToBoolean)
      println("res_ready: " + dut.io.resultChannel.ready.peek().litToBoolean)
      println("res_valid: " + dut.io.resultChannel.valid.peek().litToBoolean)

      dut.io.resultChannel.valid.expect(true.B)
      dut.io.resultChannel.bits.expect((cyclesToRun + 3).U)

      dut.clock.step()
      cycles += 1
      println("one cycle after")
      println("cycles: " + cycles)
      println("result bits: " + dut.io.resultChannel.bits.peek().litValue)
      println("start computation: " + dut.io.debugComputation.peek().litToBoolean)
      println("done with computation: " + dut.io.debugDone.peek().litToBoolean)

      println("in_ready: " + dut.io.inputChannel.ready.peek().litToBoolean)
      println("in_valid: " + dut.io.inputChannel.valid.peek().litToBoolean)
      println("res_ready: " + dut.io.resultChannel.ready.peek().litToBoolean)
      println("res_valid: " + dut.io.resultChannel.valid.peek().litToBoolean)

      dut.io.resultChannel.ready.poke(true.B)
      dut.io.inputChannel.bits.poke(2.U)

      for (i <- 0 until 8) {
        dut.clock.step()
        cycles += 1
        println("cycles: " + cycles)
        println("result bits: " + dut.io.resultChannel.bits.peek().litValue)
        println("start computation: " + dut.io.debugComputation.peek().litToBoolean)
        println("done with computation: " + dut.io.debugDone.peek().litToBoolean)

        println("in_ready: " + dut.io.inputChannel.ready.peek().litToBoolean)
        println("in_valid: " + dut.io.inputChannel.valid.peek().litToBoolean)
        println("res_ready: " + dut.io.resultChannel.ready.peek().litToBoolean)
        println("res_valid: " + dut.io.resultChannel.valid.peek().litToBoolean)
      }
    }
  }
}