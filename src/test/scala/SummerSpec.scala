import org.scalatest.freespec.AnyFreeSpec
import chisel3._
import chiseltest._

class SummerSpec extends AnyFreeSpec with ChiselScalatestTester {
  "Summer should calculate correctly" in {
    test(new Summer(8, 4, 4)) { dut =>
      dut.io.inputs.foreach { row =>
        row.foreach { element =>
          element.poke(1.U)
        }
      }
      dut.io.result.expect(16.U)
    }
  }

  "Summer should calculate correctly for different inputs" in {
    test(new Summer(8, 4, 4)) { dut =>
      dut.io.inputs.foreach { row =>
        row.foreach { element =>
          element.poke(2.U)
        }
      }
      dut.io.result.expect(32.U)
    }
  }
}
