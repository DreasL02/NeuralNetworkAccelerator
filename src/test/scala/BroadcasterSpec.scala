import chisel3._
import chiseltest._
import operators.Broadcaster
import org.scalatest.freespec.AnyFreeSpec

class BroadcasterSpec extends AnyFreeSpec with ChiselScalatestTester {

  val test1 = Array((1, 1, 1, 1), (2, 3, 4, 5))
  val test1Data = Array(Array(Array(Array(42))))


  val test2 = Array((1, 1, 1, 5), (2, 3, 4, 5))
  val test2Data = Array(Array(Array(Array(0, 1, 2, 3, 4))))

  "operators.Broadcaster should broadcast correctly (scalar)" in {
    test(new Broadcaster(8, test1(0), test1(1))) { dut =>
      for (i <- 0 until test1(0)._1) {
        for (j <- 0 until test1(0)._2) {
          for (k <- 0 until test1(0)._3) {
            for (l <- 0 until test1(0)._4) {
              dut.io.inputChannel.bits(i)(j)(k)(l).poke(test1Data(i)(j)(k)(l).U)
            }
          }
        }
      }
      dut.io.inputChannel.valid.poke(true.B)
      dut.io.outputChannel.ready.poke(true.B)
      dut.clock.step()
      dut.io.outputChannel.valid.expect(true.B)
      for (i <- 0 until test1(1)._1) {
        for (j <- 0 until test1(1)._2) {
          for (k <- 0 until test1(1)._3) {
            for (l <- 0 until test1(1)._4) {
              dut.io.outputChannel.bits(i)(j)(k)(l).expect(test1Data(i % test1(0)._1)(j % test1(0)._2)(k % test1(0)._3)(l % test1(0)._4).U)
              dut.io.outputChannel.bits(i)(j)(k)(l).expect(42.U) // to verify that the value is broadcasted
            }
          }
        }
      }
    }
  }

  "operators.Broadcaster should broadcast correctly (vector)" in {
    test(new Broadcaster(8, test2(0), test2(1))) { dut =>
      for (i <- 0 until test2(0)._1) {
        for (j <- 0 until test2(0)._2) {
          for (k <- 0 until test2(0)._3) {
            for (l <- 0 until test2(0)._4) {
              dut.io.inputChannel.bits(i)(j)(k)(l).poke(test2Data(i)(j)(k)(l).U)
            }
          }
        }
      }
      dut.io.inputChannel.valid.poke(true.B)
      dut.io.outputChannel.ready.poke(true.B)
      dut.clock.step()
      dut.io.outputChannel.valid.expect(true.B)
      for (i <- 0 until test2(1)._1) {
        for (j <- 0 until test2(1)._2) {
          for (k <- 0 until test2(1)._3) {
            for (l <- 0 until test2(1)._4) {
              dut.io.outputChannel.bits(i)(j)(k)(l).expect(test2Data(i % test2(0)._1)(j % test2(0)._2)(k % test2(0)._3)(l % test2(0)._4).U)
            }
          }
        }
      }
    }
  }
}
