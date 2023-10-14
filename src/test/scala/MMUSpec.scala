import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class MMUSpec extends AnyFreeSpec with ChiselScalatestTester {
  "Matrix Multiplication Unit should behave correctly" in {
    test(new MatrixMultiplicationUnit(w = 8, dimension = 4)) { dut =>
      
    }
  }
}