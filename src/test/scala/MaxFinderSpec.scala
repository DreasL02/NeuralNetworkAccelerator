import org.scalatest.freespec.AnyFreeSpec
import chisel3._
import chiseltest._

class MaxFinderSpec extends AnyFreeSpec with ChiselScalatestTester {
  "MaxFinder should calculate correctly" in {
    test(new MaxFinder(8, 4, 4)) { dut =>
      dut.io.inputs.foreach { row =>
        row.foreach { element =>
          element.poke(1.U)
        }
      }
      dut.io.result.expect(1.U)
    }
  }

  "MaxFinder should calculate correctly for different inputs" in {
    test(new MaxFinder(8, 4, 4)) { dut =>
      dut.io.inputs.foreach { row =>
        row.foreach { element =>
          element.poke(2.U)
        }
      }
      dut.io.result.expect(2.U)
    }
  }

  "MaxFinder should calculate correctly for a 2x2 matrix" in {
    test(new MaxFinder(8, 2, 2)) { dut =>
      dut.io.inputs(0)(0).poke(1.U)
      dut.io.inputs(0)(1).poke(2.U)
      dut.io.inputs(1)(0).poke(3.U)
      dut.io.inputs(1)(1).poke(4.U)
      dut.io.result.expect(4.U)
    }
  }

}
