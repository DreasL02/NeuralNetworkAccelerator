
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import scala_utils.MatrixUtils._
import scala_utils.FixedPointConversion._
import scala_utils.RandomData.randomMatrix

class MatMul4dSpec extends AnyFreeSpec with ChiselScalatestTester {

  val inputDimensions = (1, 3, 3, 2)
  val weightDimensions = (1, 3, 2, 4)

  val outputDimensions = (1, 3, 3, 4)
  val w = 8
  val wResult = 32

  var a = Array(
    Array(
      Array(
        Array(6, 3),
        Array(7, 4),
        Array(6, 9)
      ),
      Array(
        Array(2, 6),
        Array(7, 4),
        Array(6, 9)
      ),
      Array(
        Array(7, 2),
        Array(5, 4),
        Array(1, 7)
      )
    )
  )

  var b = Array(
    Array(
      Array(
        Array(5, 1, 4, 0),
        Array(9, 5, 8, 0)
      ),
      Array(
        Array(9, 2, 6, 3),
        Array(8, 2, 4, 2)
      ),
      Array(
        Array(6, 4, 8, 6),
        Array(1, 3, 8, 1)
      )
    )
  )

  var c = Array(
    Array(
      Array(
        Array(57, 21, 48, 0),
        Array(71, 27, 60, 0),
        Array(111, 51, 96, 0)
      ),
      Array(
        Array(66, 16, 36, 18),
        Array(95, 22, 58, 29),
        Array(83, 20, 46, 23)
      ),
      Array(
        Array(44, 34, 72, 44),
        Array(34, 32, 72, 34),
        Array(13, 25, 64, 13)
      )
    )
  )

  val aU = convertIntTensorToFixedTensor(a, 0, w, signed = true)
  val bU = convertIntTensorToFixedTensor(b, 0, w, signed = true)

  val cU = convertIntTensorToFixedTensor(c, 0, wResult, signed = true)

  println("A")
  println(tensorToString(aU))

  println("B")
  println(tensorToString(bU))

  println("C")
  println(tensorToString(cU))

  "4d MatMul" in {
    test(new MatMul4d(w, wResult, inputDimensions, weightDimensions, signed = true, enableDebuggingIO = true)) { dut =>
      dut.io.inputChannel.valid.poke(true.B)
      dut.io.weightChannel.valid.poke(true.B)
      dut.io.outputChannel.ready.poke(true.B)

      for (i <- 0 until inputDimensions._1) {
        for (j <- 0 until inputDimensions._2) {
          for (k <- 0 until inputDimensions._3) {
            for (l <- 0 until inputDimensions._4) {
              dut.io.inputChannel.bits(i)(j)(k)(l).poke(aU(i)(j)(k)(l).asUInt)
            }
          }
        }
      }

      for (i <- 0 until weightDimensions._1) {
        for (j <- 0 until weightDimensions._2) {
          for (k <- 0 until weightDimensions._3) {
            for (l <- 0 until weightDimensions._4) {
              dut.io.weightChannel.bits(i)(j)(k)(l).poke(bU(i)(j)(k)(l).asUInt)
            }
          }
        }
      }

      var cycle = 0
      while (!dut.io.outputChannel.valid.peek().litToBoolean) {
        dut.clock.step()
        cycle += 1

        if (cycle > 100) {
          fail("Test did not finish in 100 cycles")
        }
      }

      val results = Array.ofDim[BigInt](outputDimensions._1, outputDimensions._2, outputDimensions._3, outputDimensions._4)
      for (i <- 0 until outputDimensions._1) {
        for (j <- 0 until outputDimensions._2) {
          for (k <- 0 until outputDimensions._3) {
            for (l <- 0 until outputDimensions._4) {
              results(i)(j)(k)(l) = dut.io.outputChannel.bits(i)(j)(k)(l).peek().litValue
            }
          }
        }
      }

      println("Cycles")
      println(cycle)

      println("Results")
      println(tensorToString(results))
    }
  }

}