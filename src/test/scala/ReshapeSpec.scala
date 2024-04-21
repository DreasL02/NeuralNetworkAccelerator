
import chisel3._
import chiseltest._
import operators.Reshape
import org.scalatest.freespec.AnyFreeSpec

class ReshapeSpec extends AnyFreeSpec with ChiselScalatestTester {
  // 2 x 2 x 4 x 4
  val inputs = Array(
    Array(
      Array(
        Array(0, 1, 2, 3),
        Array(4, 5, 6, 7),
        Array(8, 9, 10, 11),
        Array(12, 13, 14, 15)
      ),
      Array(
        Array(16, 17, 18, 19),
        Array(20, 21, 22, 23),
        Array(24, 25, 26, 27),
        Array(28, 29, 30, 31)
      )
    ),
    Array(
      Array(
        Array(32, 33, 34, 35),
        Array(36, 37, 38, 39),
        Array(40, 41, 42, 43),
        Array(44, 45, 46, 47)
      ),
      Array(
        Array(48, 49, 50, 51),
        Array(52, 53, 54, 55),
        Array(56, 57, 58, 59),
        Array(60, 61, 62, 63)
      )
    )
  )

  // expected output: 1 x 1 x 4 x 16
  val expectedOutput = Array(
    Array(
      Array(
        Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
        Array(16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31),
        Array(32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47),
        Array(48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63)
      )
    )
  )

  "operators.Reshape should reshape correctly" in {
    test(new Reshape(w = 8, inputDimensions = (2, 2, 4, 4), shapeDimensions = (1, 1, 1, 1), newDimensions = (1, 1, 4, 16))) {
      dut =>
        dut.io.inputChannel.valid.poke(true.B)
        dut.io.outputChannel.ready.poke(true.B)
        for (i <- inputs.indices) {
          for (j <- inputs(i).indices) {
            for (k <- inputs(i)(j).indices) {
              for (l <- inputs(i)(j)(k).indices) {
                dut.io.inputChannel.bits(i)(j)(k)(l).poke(inputs(i)(j)(k)(l))
              }
            }
          }
        }

        var cycle = 0
        while (!dut.io.outputChannel.valid.peek().litToBoolean) {
          dut.clock.step()
          cycle += 1
        }

        for (i <- expectedOutput.indices) {
          for (j <- expectedOutput(i).indices) {
            for (k <- expectedOutput(i)(j).indices) {
              for (l <- expectedOutput(i)(j)(k).indices) {
                dut.io.outputChannel.bits(i)(j)(k)(l).expect(expectedOutput(i)(j)(k)(l).U)
              }
            }
          }
        }
    }
  }
}
