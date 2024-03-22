
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import scala_utils.FixedPointConversion.{fixedToFloat, floatToFixed}

class ConvMatMulSpec extends AnyFreeSpec with ChiselScalatestTester {

  // Using tests from https://github.com/onnx/onnx/blob/main/docs/Operators.md#Conv

  val inputs = Array(
    Array(0, 1, 2, 3, 4),
    Array(5, 6, 7, 8, 9),
    Array(10, 11, 12, 13, 14),
    Array(15, 16, 17, 18, 19),
    Array(20, 21, 22, 23, 24)
  )

  val weights = Array(
    Array(1, 1, 1),
    Array(1, 1, 1),
    Array(1, 1, 1)
  )

  val toPrintPadding = false
  val toPrintNoPadding = true

  /*
  "ConvMatMul should calculate correctly when padding" in {
    test(new ConvMatMul(
      w = 8,
      wResult = 32,
      inputDimensions = (inputs.length, inputs(0).length),
      kernelDimensions = (weights.length, weights(0).length),
      signed = true,
      strides = (1, 1),
      pads = (1, 1),
      true
    )) {
      dut =>
        val expectedOutput = Array(
          Array(12, 21, 27, 33, 24),
          Array(33, 54, 63, 72, 51),
          Array(63, 99, 108, 117, 81),
          Array(93, 144, 153, 162, 111),
          Array(72, 111, 117, 123, 84)
        )


        dut.io.inputChannel.valid.poke(true.B)
        dut.io.kernelChannel.valid.poke(true.B)
        for (i <- inputs.indices) {
          for (j <- inputs(i).indices) {
            dut.io.inputChannel.bits(i)(j).poke(inputs(i)(j))
          }
        }
        for (i <- weights.indices) {
          for (j <- weights(i).indices) {
            dut.io.kernelChannel.bits(i)(j).poke(weights(i)(j))
          }
        }
        dut.io.outputChannel.ready.poke(true.B)

        var cycle = 0
        while (!dut.io.outputChannel.valid.peek().litToBoolean) {
          dut.clock.step()
          cycle += 1

          // print result matrix
          if (toPrintPadding) {
            println("Cycle: " + cycle)
            for (i <- expectedOutput.indices) {
              for (j <- expectedOutput(i).indices) {
                val result = dut.io.outputChannel.bits(i)(j).peek().litValue
                print("%d ".format(result))
              }
              println()
            }
          }


          if (cycle > 100) {
            fail("Timeout")
          }
        }


        for (i <- expectedOutput.indices) {
          for (j <- expectedOutput(i).indices) {
            val result = dut.io.outputChannel.bits(i)(j).peek().litValue
            assert(result == expectedOutput(i)(j))
          }
        }
    }
  }

   */

  /*
  "ConvMatMul should calculate correctly when no padding" in {
    test(new ConvMatMul(
      w = 8,
      wResult = 32,
      inputDimensions = (inputs.length, inputs(0).length),
      kernelDimensions = (weights.length, weights(0).length),
      signed = true,
      strides = (1, 1),
      pads = (0, 0),
      true
    )) {
      dut =>
        val expectedOutput = Array(
          Array(54, 63, 72),
          Array(99, 108, 117),
          Array(144, 153, 162)
        )

        dut.io.inputChannel.valid.poke(true.B)
        dut.io.kernelChannel.valid.poke(true.B)
        for (i <- inputs.indices) {
          for (j <- inputs(i).indices) {
            dut.io.inputChannel.bits(i)(j).poke(inputs(i)(j))
          }
        }
        for (i <- weights.indices) {
          for (j <- weights(i).indices) {
            dut.io.kernelChannel.bits(i)(j).poke(weights(i)(j))
          }
        }
        dut.io.outputChannel.ready.poke(true.B)

        var cycle = 0
        while (!dut.io.outputChannel.valid.peek().litToBoolean) {
          dut.clock.step()
          cycle += 1

          // print result matrix
          if (toPrintPadding) {
            println("Cycle: " + cycle)
            for (i <- expectedOutput.indices) {
              for (j <- expectedOutput(i).indices) {
                val result = dut.io.outputChannel.bits(i)(j).peek().litValue
                print("%d ".format(result))
              }
              println()
            }
          }
          if (cycle > 100) {
            fail("Timeout")
          }
        }

        for (i <- expectedOutput.indices) {
          for (j <- expectedOutput(i).indices) {
            val result = dut.io.outputChannel.bits(i)(j).peek().litValue
            assert(result == expectedOutput(i)(j))
          }
        }
    }
  }

   */

  val inputs2 = Array(Array(1, 2, 3), Array(4, 5, 6))
  val weights2 = Array(Array(10, 20), Array(30, 40))

  "ConvMatMul should calculate correctly example from Github" in {
    test(new ConvMatMul(
      w = 8,
      wResult = 32,
      inputDimensions = (inputs2.length, inputs2(0).length),
      kernelDimensions = (weights2.length, weights2(0).length),
      signed = true,
      strides = (1, 1),
      pads = (1, 1),
      true
    )) {
      dut =>
        val expectedOutput = Array(
          Array(10, 40, 70, 60),
          Array(70, 230, 330, 240),
          Array(120, 310, 380, 240)
        )

        dut.io.inputChannel.valid.poke(true.B)
        dut.io.kernelChannel.valid.poke(true.B)
        for (i <- inputs.indices) {
          for (j <- inputs(i).indices) {
            dut.io.inputChannel.bits(i)(j).poke(inputs(i)(j))
          }
        }
        for (i <- weights.indices) {
          for (j <- weights(i).indices) {
            dut.io.kernelChannel.bits(i)(j).poke(weights(i)(j))
          }
        }
        dut.io.outputChannel.ready.poke(true.B)

        var cycle = 0
        while (!dut.io.outputChannel.valid.peek().litToBoolean) {
          dut.clock.step()
          cycle += 1

          // print result matrix
          if (toPrintPadding) {
            println("Cycle: " + cycle)
            for (i <- expectedOutput.indices) {
              for (j <- expectedOutput(i).indices) {
                val result = dut.io.outputChannel.bits(i)(j).peek().litValue
                print("%d ".format(result))
              }
              println()
            }
          }
          if (cycle > 100) {
            fail("Timeout")
          }
        }

        for (i <- expectedOutput.indices) {
          for (j <- expectedOutput(i).indices) {
            val result = dut.io.outputChannel.bits(i)(j).peek().litValue
            assert(result == expectedOutput(i)(j))
          }
        }
    }
  }
}
