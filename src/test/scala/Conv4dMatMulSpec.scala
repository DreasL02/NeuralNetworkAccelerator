
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import scala_utils.FixedPointConversion.{fixedToFloat, floatToFixed}

class Conv4dMatMulSpec extends AnyFreeSpec with ChiselScalatestTester {

  // Using tests from https://github.com/onnx/onnx/blob/main/docs/Operators.md#Conv

  val inputs = Array(Array(Array(
    Array(0, 1, 2, 3, 4),
    Array(5, 6, 7, 8, 9),
    Array(10, 11, 12, 13, 14),
    Array(15, 16, 17, 18, 19),
    Array(20, 21, 22, 23, 24)
  )))

  val weights = Array(Array(Array(
    Array(1, 1, 1),
    Array(1, 1, 1),
    Array(1, 1, 1)
  )))

  val toPrintPadding = true
  val toPrintNoPadding = true


  "Convolution should calculate correctly when padding" in {
    test(new Conv4dMatmul(
      w = 8,
      wResult = 32,
      inputDimensions = (inputs.length, inputs(0).length, inputs(0)(0).length, inputs(0)(0)(0).length),
      kernelDimensions = (weights.length, weights(0).length, weights(0)(0).length, weights(0)(0)(0).length),
      signed = true,
      strides = (1, 1),
      pads = (1, 1))) {
      dut =>
        val expectedOutput = Array(Array(Array(
          Array(12, 21, 27, 33, 24),
          Array(33, 54, 63, 72, 51),
          Array(63, 99, 108, 117, 81),
          Array(93, 144, 153, 162, 111),
          Array(72, 111, 117, 123, 84)
        )))


        dut.io.inputChannel.valid.poke(true.B)
        dut.io.kernelChannel.valid.poke(true.B)
        for (i <- inputs.indices) {
          for (j <- inputs(i).indices) {
            for (k <- inputs(i)(j).indices) {
              for (l <- inputs(i)(j)(k).indices) {
                dut.io.inputChannel.bits(i)(j)(k)(l).poke(inputs(i)(j)(k)(l))
              }
            }
          }
        }
        for (i <- weights.indices) {
          for (j <- weights(i).indices) {
            for (k <- weights(i)(j).indices) {
              for (l <- weights(i)(j)(k).indices) {
                dut.io.kernelChannel.bits(i)(j)(k)(l).poke(weights(i)(j)(k)(l))
              }
            }
          }
        }

        val debugIm2ColInput = dut.io.debugInputIm2Col.get
        val debugIm2ColKernel = dut.io.debugKernelIm2Col.get

        println("Im2Col input")
        for (i <- debugIm2ColInput.indices) {
          for (j <- debugIm2ColInput(i).indices) {
            for (k <- debugIm2ColInput(i)(j).indices) {
              val result = debugIm2ColInput(i)(j)(k).peek().litValue
              print("%d ".format(result))
            }
            println()
          }
          println()
        }

        println("Im2Col kernel")
        for (i <- debugIm2ColKernel.indices) {
          for (j <- debugIm2ColKernel(i).indices) {
            val result = debugIm2ColKernel(i)(j).peek().litValue
            print("%d ".format(result))
          }
          println()
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
                for (k <- expectedOutput(i)(j).indices) {
                  for (l <- expectedOutput(i)(j)(k).indices) {
                    val result = dut.io.outputChannel.bits(i)(j)(k)(l).peek().litValue
                    print("%d ".format(result))
                  }
                  println()
                }
                println()
              }
              println()
            }
          }


          if (cycle > 100) {
            fail("Timeout")
          }
        }

        if (toPrintPadding) {
          println("Done in cycle: %d".format(cycle))
        }

        for (i <- expectedOutput.indices) {
          for (j <- expectedOutput(i).indices) {
            for (k <- expectedOutput(i)(j).indices) {
              for (l <- expectedOutput(i)(j)(k).indices) {
                val result = dut.io.outputChannel.bits(i)(j)(k)(l).peek().litValue
                assert(result == expectedOutput(i)(j)(k)(l))
              }
            }
          }
        }
    }
  }


  "Convolution should calculate correctly when no padding" in {
    test(new Conv4dMatmul(
      w = 8,
      wResult = 32,
      inputDimensions = (inputs.length, inputs(0).length, inputs(0)(0).length, inputs(0)(0)(0).length),
      kernelDimensions = (weights.length, weights(0).length, weights(0)(0).length, weights(0)(0)(0).length),
      signed = true,
      strides = (1, 1),
      pads = (0, 0))) {
      dut =>
        val expectedOutput = Array(Array(Array(
          Array(54, 63, 72),
          Array(99, 108, 117),
          Array(144, 153, 162)
        )))

        dut.io.inputChannel.valid.poke(true.B)
        dut.io.kernelChannel.valid.poke(true.B)


        for (i <- inputs.indices) {
          for (j <- inputs(i).indices) {
            for (k <- inputs(i)(j).indices) {
              for (l <- inputs(i)(j)(k).indices) {
                dut.io.inputChannel.bits(i)(j)(k)(l).poke(inputs(i)(j)(k)(l))
              }
            }
          }
        }

        for (i <- weights.indices) {
          for (j <- weights(i).indices) {
            for (k <- weights(i)(j).indices) {
              for (l <- weights(i)(j)(k).indices) {
                dut.io.kernelChannel.bits(i)(j)(k)(l).poke(weights(i)(j)(k)(l))
              }
            }
          }
        }

        val debugIm2ColInput = dut.io.debugInputIm2Col.get
        val debugIm2ColKernel = dut.io.debugKernelIm2Col.get

        println("Im2Col input")
        for (i <- debugIm2ColInput.indices) {
          for (j <- debugIm2ColInput(i).indices) {
            for (k <- debugIm2ColInput(i)(j).indices) {
              val result = debugIm2ColInput(i)(j)(k).peek().litValue
              print("%d ".format(result))
            }
            println()
          }
          println()
        }

        println("Im2Col kernel")
        for (i <- debugIm2ColKernel.indices) {
          for (j <- debugIm2ColKernel(i).indices) {
            val result = debugIm2ColKernel(i)(j).peek().litValue
            print("%d ".format(result))
          }
          println()
        }


        dut.io.outputChannel.ready.poke(true.B)

        var cycle = 0

        while (!dut.io.outputChannel.valid.peek().litToBoolean) {
          dut.clock.step()
          cycle += 1

          if (toPrintNoPadding) {
            println("Cycle: " + cycle)
            for (i <- expectedOutput.indices) {
              for (j <- expectedOutput(i).indices) {
                for (k <- expectedOutput(i)(j).indices) {
                  for (l <- expectedOutput(i)(j)(k).indices) {
                    val result = dut.io.outputChannel.bits(i)(j)(k)(l).peek().litValue
                    print("%d ".format(result))
                  }
                  println()
                }
                println()
              }
              println()
            }
          }

          if (cycle > 100) {
            fail("Timeout")
          }
        }

        if (toPrintNoPadding) {
          println("Done in cycle: %d".format(cycle))
        }

        for (i <- expectedOutput.indices) {
          for (j <- expectedOutput(i).indices) {
            for (k <- expectedOutput(i)(j).indices) {
              for (l <- expectedOutput(i)(j)(k).indices) {
                val result = dut.io.outputChannel.bits(i)(j)(k)(l).peek().litValue
                assert(result == expectedOutput(i)(j)(k)(l))
              }
            }
          }
        }
    }
  }


  // 1 x 3 x 5 x 5
  val test3Input = Array(
    Array(
      Array(
        Array(1, 0, 1, 0, 2),
        Array(1, 1, 3, 2, 1),
        Array(1, 1, 0, 1, 1),
        Array(2, 3, 2, 1, 3),
        Array(0, 2, 0, 1, 0)
      ),
      Array(
        Array(1, 0, 0, 1, 0),
        Array(2, 0, 1, 2, 0),
        Array(3, 1, 1, 3, 0),
        Array(0, 3, 0, 3, 2),
        Array(1, 0, 3, 2, 1)
      ),
      Array(
        Array(2, 0, 1, 2, 1),
        Array(3, 3, 1, 3, 2),
        Array(2, 1, 1, 1, 0),
        Array(3, 1, 3, 2, 0),
        Array(1, 1, 2, 1, 1)
      )
    )
  )

  // 1 x 3 x 3 x 3
  val test3Weights = Array(Array(
    Array(
      Array(0, 1, 0),
      Array(0, 0, 2),
      Array(0, 1, 0)
    ),
    Array(
      Array(2, 1, 0),
      Array(0, 0, 0),
      Array(0, 3, 0)
    ),
    Array(
      Array(1, 0, 0),
      Array(1, 0, 0),
      Array(0, 0, 2)
    )
  ))

  "Convolution should calculate for 3D example" in {
    test(new Conv4dMatmul(
      w = 8,
      wResult = 32,
      inputDimensions = (test3Input.length, test3Input(0).length, test3Input(0)(0).length, test3Input(0)(0)(0).length),
      kernelDimensions = (test3Weights.length, test3Weights(0).length, test3Weights(0)(0).length, test3Weights(0)(0)(0).length),
      signed = true,
      strides = (1, 1),
      pads = (0, 0))) {
      dut =>
        val expectedOutput = Array(
          Array(
            Array(
              Array(19, 13, 15),
              Array(28, 16, 20),
              Array(23, 18, 25)
            )
          )
        )

        dut.io.inputChannel.valid.poke(true.B)
        dut.io.kernelChannel.valid.poke(true.B)

        for (i <- test3Input.indices) {
          for (j <- test3Input(i).indices) {
            for (k <- test3Input(i)(j).indices) {
              for (l <- test3Input(i)(j)(k).indices) {
                dut.io.inputChannel.bits(i)(j)(k)(l).poke(test3Input(i)(j)(k)(l))
              }
            }
          }
        }

        for (i <- test3Weights.indices) {
          for (j <- test3Weights(i).indices) {
            for (k <- test3Weights(i)(j).indices) {
              for (l <- test3Weights(i)(j)(k).indices) {
                dut.io.kernelChannel.bits(i)(j)(k)(l).poke(test3Weights(i)(j)(k)(l))
              }
            }
          }
        }

        dut.io.outputChannel.ready.poke(true.B)

        val debugIm2ColInput = dut.io.debugInputIm2Col.get
        val debugIm2ColKernel = dut.io.debugKernelIm2Col.get

        println("Im2Col input")
        for (i <- debugIm2ColInput.indices) {
          for (j <- debugIm2ColInput(i).indices) {
            for (k <- debugIm2ColInput(i)(j).indices) {
              val result = debugIm2ColInput(i)(j)(k).peek().litValue
              print("%d ".format(result))
            }
            println()
          }
          println()
        }

        println("Im2Col kernel")
        for (i <- debugIm2ColKernel.indices) {
          for (j <- debugIm2ColKernel(i).indices) {
            val result = debugIm2ColKernel(i)(j).peek().litValue
            print("%d ".format(result))
          }
          println()
        }

        var cycle = 0

        while (!dut.io.outputChannel.valid.peek().litToBoolean) {
          dut.clock.step()
          cycle += 1

          if (toPrintNoPadding) {
            println("Cycle: " + cycle)
            for (i <- expectedOutput.indices) {
              for (j <- expectedOutput(i).indices) {
                for (k <- expectedOutput(i)(j).indices) {
                  for (l <- expectedOutput(i)(j)(k).indices) {
                    val result = dut.io.outputChannel.bits(i)(j)(k)(l).peek().litValue
                    print("%d ".format(result))
                  }
                  println()
                }
                println()
              }
              println()
            }
          }

          if (cycle > 100) {
            fail("Timeout")
          }
        }
    }
  }
}
