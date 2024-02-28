import chisel3._
import chiseltest._
import module_utils.Adders
import org.scalatest.freespec.AnyFreeSpec

class AddersSpec extends AnyFreeSpec with ChiselScalatestTester {
  "Should add two matrices correctly" in {
    test(new Adders(16, 3, 3)) { dut =>
      for (i <- 0 until 3) {
        for (j <- 0 until 3) {
          dut.io.operandA(i)(j).poke(11.U)
          dut.io.operandB(i)(j).poke(((Math.pow(2, 16) - 1).toInt).U)
        }
      }

      for (i <- 0 until 3) {
        for (j <- 0 until 3) {
          dut.io.result(i)(j).expect(10.U)
        }
      }
    }
  }
}
