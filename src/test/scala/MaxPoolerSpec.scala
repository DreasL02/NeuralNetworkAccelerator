
import org.scalatest.freespec.AnyFreeSpec
import chisel3._
import chiseltest._

// max pooler has the following parameters: w: Int, kernelShape: (Int, Int), pads: (Int, Int), strides: (Int, Int), xDimension: Int, yDimension: Int
// and does the max pool operation used in CNNs
// This file contains the tests for the MaxPooler module
class MaxPoolerSpec extends AnyFreeSpec with ChiselScalatestTester {
  "MaxPooler should calculate correctly" in {
    test(new MaxPooler(8, (2, 2), (0, 0), (2, 2), 4, 4)) { dut =>
      dut.io.inputs.foreach { row =>
        row.foreach { element =>
          element.poke(1.U)
        }
      }
      dut.io.result.foreach { row =>
        row.foreach { element =>
          element.expect(1.U)
        }
      }
    }
  }

  "MaxPooler should calculate correctly for different inputs" in {
    test(new MaxPooler(8, (2, 2), (0, 0), (2, 2), 4, 4)) { dut =>
      dut.io.inputs.foreach { row =>
        row.foreach { element =>
          element.poke(2.U)
        }
      }
      dut.io.result.foreach { row =>
        row.foreach { element =>
          element.expect(2.U)
        }
      }
    }
  }

  "MaxPooler should calculate correctly for a 2x2 matrix" in {
    test(new MaxPooler(8, (2, 2), (0, 0), (2, 2), 2, 2)) { dut =>
      dut.io.inputs(0)(0).poke(1.U)
      dut.io.inputs(0)(1).poke(2.U)
      dut.io.inputs(1)(0).poke(3.U)
      dut.io.inputs(1)(1).poke(4.U)
      dut.io.result(0)(0).expect(4.U)
    }
  }

  "MaxPooler should calculate correctly for a 4x4 matrix" in {
    test(new MaxPooler(8, (2, 2), (0, 0), (2, 2), 4, 4)) { dut =>
      val matrix = Array(
        Array(1, 2, 3, 4),
        Array(5, 6, 7, 8),
        Array(9, 10, 16, 12),
        Array(13, 14, 15, 11)
      )

      for (i <- 0 until 4) {
        for (j <- 0 until 4) {
          dut.io.inputs(i)(j).poke(matrix(i)(j).U)
        }
      }

      dut.io.result(0)(0).expect(6.U)
      dut.io.result(0)(1).expect(8.U)
      dut.io.result(1)(0).expect(14.U)
      dut.io.result(1)(1).expect(16.U)
    }
  }
}