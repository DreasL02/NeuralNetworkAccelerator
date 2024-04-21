
import org.scalatest.freespec.AnyFreeSpec
import chisel3._
import chiseltest._
import operators.MaxPool


// Test cases from:
// https://www.quora.com/What-is-max-pooling-in-convolutional-neural-networks
class MaxPoolSpec extends AnyFreeSpec with ChiselScalatestTester {
  val toPrint = false
  "operators.MaxPool should calculate correctly for 4x4 input, 2x2 kernel and (2,2) strides" in {
    val matrix = Array(
      Array(2, 3, 4, 0),
      Array(1, 5, 3, 2),
      Array(0, 4, 2, 3),
      Array(1, 0, 6, 1)
    )

    val expected = Array(
      Array(5, 4),
      Array(4, 6)
    )

    test(new MaxPool(8, matrix.length, matrix(0).length, (2, 2), (0, 0), (2, 2))) { dut =>
      for (i <- 0 until matrix.length) {
        for (j <- 0 until matrix(i).length) {
          dut.io.inputChannel.bits(i)(j).poke(matrix(i)(j).U)
        }
      }
      dut.io.inputChannel.valid.poke(true.B)
      dut.io.outputChannel.ready.poke(true.B)
      dut.io.outputChannel.valid.expect(true.B)

      val result = dut.io.outputChannel.bits

      if (toPrint) {
        for (i <- 0 until result.length) {
          for (j <- 0 until result(i).length) {
            print(result(i)(j).peek().litValue)
            print(" ")
          }
          println()
        }
        println()
      }

      for (i <- 0 until result.length) {
        for (j <- 0 until result(i).length) {
          result(i)(j).expect(expected(i)(j).U)
        }
      }
    }
  }

  "operators.MaxPool should calculate correctly for 4x4 input, 3x3 kernel and (1,1) strides" in {
    val matrix = Array(
      Array(2, 3, 4, 0),
      Array(1, 5, 3, 2),
      Array(0, 4, 2, 3),
      Array(1, 0, 6, 1)
    )

    val expected = Array(
      Array(5, 5),
      Array(6, 6),
    )

    test(new MaxPool(8, matrix.length, matrix(0).length, (3, 3), (0, 0), (1, 1))) { dut =>
      for (i <- 0 until matrix.length) {
        for (j <- 0 until matrix(i).length) {
          dut.io.inputChannel.bits(i)(j).poke(matrix(i)(j).U)
        }
      }
      dut.io.inputChannel.valid.poke(true.B)
      dut.io.outputChannel.ready.poke(true.B)
      dut.io.outputChannel.valid.expect(true.B)

      val result = dut.io.outputChannel.bits

      if (toPrint) {
        for (i <- 0 until result.length) {
          for (j <- 0 until result(i).length) {
            print(result(i)(j).peek().litValue)
            print(" ")
          }
          println()
        }
        println()
      }

      for (i <- 0 until result.length) {
        for (j <- 0 until result(i).length) {
          result(i)(j).expect(expected(i)(j).U)
        }
      }
    }
  }

  "operators.MaxPool should calculate correctly for 4x4 input, 3x3 kernel and (2,2) strides" in {
    val matrix = Array(
      Array(2, 3, 4, 0),
      Array(1, 5, 3, 2),
      Array(0, 4, 2, 3),
      Array(1, 0, 6, 1)
    )

    val expected = Array(
      Array(5),
    )

    test(new MaxPool(8, matrix.length, matrix(0).length, (3, 3), (0, 0), (2, 2))) { dut =>
      for (i <- 0 until matrix.length) {
        for (j <- 0 until matrix(i).length) {
          dut.io.inputChannel.bits(i)(j).poke(matrix(i)(j).U)
        }
      }
      dut.io.inputChannel.valid.poke(true.B)
      dut.io.outputChannel.ready.poke(true.B)
      dut.io.outputChannel.valid.expect(true.B)

      val result = dut.io.outputChannel.bits

      if (toPrint) {
        for (i <- 0 until result.length) {
          for (j <- 0 until result(i).length) {
            print(result(i)(j).peek().litValue)
            print(" ")
          }
          println()
        }
        println()
      }

      for (i <- 0 until result.length) {
        for (j <- 0 until result(i).length) {
          result(i)(j).expect(expected(i)(j).U)
        }
      }
    }
  }
}

