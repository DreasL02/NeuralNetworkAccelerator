
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class Interfacer2Spec extends AnyFreeSpec with ChiselScalatestTester {

  val cyclesToRun = 3
  val cyclesToRun2 = 4
  "Interfacer2 should increment the input value by 1" in {
    test(new Interfacer2(w = 8, cycles1 = cyclesToRun, cycles2 = cyclesToRun2)) { dut =>
      var cycles = 0

      dut.io.inputChannel.bits.poke(3.U)
      dut.io.inputChannel.valid.poke(true.B)
      dut.io.resultChannel.ready.poke(true.B)
      dut.io.resultChannel.valid.expect(false.B)
      dut.io.resultChannel.bits.expect(0.U)

      while (!dut.io.resultChannel.valid.peek().litToBoolean) {
        println("cycles: " + cycles)
        println("result bits: " + dut.io.resultChannel.bits.peek().litValue)
        println("start computation 1: " + dut.io.debugComputation1.peek().litToBoolean)
        println("done with computation 1: " + dut.io.debugDone1.peek().litToBoolean)
        println("start computation 2: " + dut.io.debugComputation2.peek().litToBoolean)
        println("done with computation 2: " + dut.io.debugDone2.peek().litToBoolean)

        println("in_ready: " + dut.io.inputChannel.ready.peek().litToBoolean)
        println("in_valid: " + dut.io.inputChannel.valid.peek().litToBoolean)
        println("res_ready: " + dut.io.resultChannel.ready.peek().litToBoolean)
        println("res_valid: " + dut.io.resultChannel.valid.peek().litToBoolean)
        dut.clock.step()

        cycles += 1
      }
      println("cycles: " + cycles)
      println("result bits: " + dut.io.resultChannel.bits.peek().litValue)
      println("start computation 1: " + dut.io.debugComputation1.peek().litToBoolean)
      println("done with computation 1: " + dut.io.debugDone1.peek().litToBoolean)
      println("start computation 2: " + dut.io.debugComputation2.peek().litToBoolean)
      println("done with computation 2: " + dut.io.debugDone2.peek().litToBoolean)

      println("in_ready: " + dut.io.inputChannel.ready.peek().litToBoolean)
      println("in_valid: " + dut.io.inputChannel.valid.peek().litToBoolean)
      println("res_ready: " + dut.io.resultChannel.ready.peek().litToBoolean)
      println("res_valid: " + dut.io.resultChannel.valid.peek().litToBoolean)

      dut.clock.step()
      cycles += 1

      while (!dut.io.resultChannel.valid.peek().litToBoolean) {
        println("cycles: " + cycles)
        println("result bits: " + dut.io.resultChannel.bits.peek().litValue)
        println("start computation 1: " + dut.io.debugComputation1.peek().litToBoolean)
        println("done with computation 1: " + dut.io.debugDone1.peek().litToBoolean)
        println("start computation 2: " + dut.io.debugComputation2.peek().litToBoolean)
        println("done with computation 2: " + dut.io.debugDone2.peek().litToBoolean)

        println("in_ready: " + dut.io.inputChannel.ready.peek().litToBoolean)
        println("in_valid: " + dut.io.inputChannel.valid.peek().litToBoolean)
        println("res_ready: " + dut.io.resultChannel.ready.peek().litToBoolean)
        println("res_valid: " + dut.io.resultChannel.valid.peek().litToBoolean)
        dut.clock.step()

        cycles += 1
      }
      println("cycles: " + cycles)
      println("result bits: " + dut.io.resultChannel.bits.peek().litValue)
      println("start computation 1: " + dut.io.debugComputation1.peek().litToBoolean)
      println("done with computation 1: " + dut.io.debugDone1.peek().litToBoolean)
      println("start computation 2: " + dut.io.debugComputation2.peek().litToBoolean)
      println("done with computation 2: " + dut.io.debugDone2.peek().litToBoolean)

      println("in_ready: " + dut.io.inputChannel.ready.peek().litToBoolean)
      println("in_valid: " + dut.io.inputChannel.valid.peek().litToBoolean)
      println("res_ready: " + dut.io.resultChannel.ready.peek().litToBoolean)
      println("res_valid: " + dut.io.resultChannel.valid.peek().litToBoolean)
    }
  }
}