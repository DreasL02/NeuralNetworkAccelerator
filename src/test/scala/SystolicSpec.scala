
import chisel3._
import chisel3.util.Fill
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import systolic_array.SystolicArray
import utils.RandomData._
import utils.MatrixUtils._
import utils.FixedPointConversion._

class SystolicSpec extends AnyFreeSpec with ChiselScalatestTester {
  // ======= configure the test =======
  val w = 8
  val wStore = 4 * w
  val xDimension = 3
  val yDimension = 3
  val matrixCommonDimension = 3
  val fixedPoint = 3
  val signed = true.B
  val numberOfTests = 1
  val max = 1.2f
  val min = 0.0f
  val threshold = 0.1f
  val printing = Array.fill(numberOfTests)(false)

  // We can enable printing for a specific test by setting the index to true
  printing(0) = true



  // ======= configure the test end =======

  // --- Rest should be left as is ---
  val seeds = Array.fill(numberOfTests * 2)(0)
  // increment seeds for each test and matrix to get different random numbers
  for (i <- 0 until numberOfTests * 2) {
    seeds(i) = 10 * i
  }

  // for each seed, generate a random matrix and test
  for (testNum <- 0 until numberOfTests) {
    val enablePrinting = printing(testNum)
    "SystolicArray should calculate correctly for test %d".format(testNum) in {
      test(new SystolicArray(w, wStore, xDimension, yDimension)) { dut =>
        var m1f = randomMatrix(yDimension, matrixCommonDimension, min, max, seeds(testNum * 2))
        var m2f = randomMatrix(matrixCommonDimension, xDimension, min, max, seeds(testNum * 2 + 1))

        var mrf = calculateMatrixMultiplication(m1f, m2f)
        if (enablePrinting)
          printMatrixMultiplication(m1f, m2f, mrf, "GOLDEN MODEL CALCULATION IN PURE FLOATING")

        val m1 = convertFloatMatrixToFixedMatrix(m1f, fixedPoint, w, signed.litToBoolean)
        val m2 = convertFloatMatrixToFixedMatrix(m2f, fixedPoint, w, signed.litToBoolean)
        val mr = calculateMatrixMultiplication(m1, m2)

        val ms1 = convertFloatMatrixToFixedMatrix(m1f, fixedPoint, w, signed.litToBoolean)
        val ms2 = convertFloatMatrixToFixedMatrix(m2f, fixedPoint, w, signed.litToBoolean)

        if (enablePrinting)
          printMatrixMultiplication(m1, m2, mr, "GOLDEN MODEL CALCULATION IN FIXED POINT")

        m1f = convertFixedMatrixToFloatMatrix(m1, fixedPoint, w, signed.litToBoolean)
        m2f = convertFixedMatrixToFloatMatrix(m2, fixedPoint, w, signed.litToBoolean)
        mrf = calculateMatrixMultiplication(m1f, m2f)
        if (enablePrinting) {
          printMatrixMultiplication(m1f, m2f, mrf, "GOLDEN MODEL CALCULATION IN AFTER TRANSFORMATION BACK TO FLOATING")
        }

        val mm1 = convertMatrixToMappedAMatrix(ms1, xDimension)
        val mm2 = convertMatrixToMappedBMatrix(ms2, yDimension)

        dut.io.signed.poke(signed)

        if (enablePrinting) {
          println("-- a --")
          print(matrixToString(mm1))
          println("-- b --")
          print(matrixToString(mm2))
          println("----")
        }

        val max_number_of_cycles = mm1.length * mm2(0).length
        println("max_number_of_cycles: %d".format(max_number_of_cycles))
        for (cycle <- 0 until max_number_of_cycles) {
          for (i <- mm1.indices) {
            dut.io.a(i).poke(mm1(i)(mm1(0).length - 1 - cycle))
          }
          for (i <- mm2(0).indices) {
            dut.io.b(i).poke(mm2(mm2.length - 1 - cycle)(i))
          }
          dut.clock.step()

          val resultFixed: Array[Array[BigInt]] = Array.fill(mm1.length, mm2(0).length)(0)
          for (i <- mr.indices) {
            for (j <- mr(0).indices) {
              resultFixed(i)(j) = dut.io.c(j)(i).peek().litValue
            }
          }
          if (enablePrinting) {
            println("Cycle %d:".format(cycle))
            print(matrixToString(resultFixed))
          }
        }

        val resultFixed: Array[Array[BigInt]] = Array.fill(mm1.length, mm2(0).length)(0)
        for (i <- mr.indices) {
          for (j <- mr(0).indices) {
            resultFixed(i)(j) = dut.io.c(j)(i).peek().litValue
          }
        }

        // width and fixed point are doubled because the result is the sum of two fixed point numbers and no rounding is done
        val resultFloat = convertFixedMatrixToFloatMatrix(resultFixed, fixedPoint * 2, w * 2, signed.litToBoolean)


        if (enablePrinting) {
          println("---- SYSTOLIC ARRAY RESULT IN FIXED ----")
          print(matrixToString(resultFixed))
          println("---- SYSTOLIC ARRAY RESULT IN FLOAT ----")
          print(matrixToString(resultFloat))
          println()
        }

        for (i <- mrf.indices) {
          for (j <- mrf(0).indices) {
            val a = resultFloat(i)(j)
            val b = mrf(i)(j)
            var valid = false
            if (a - threshold <= b && a + threshold >= b) { //with in +-threshold of golden model
              valid = true
            }
            assert(valid, ": test (%d) : element at (%d,%d) did not match (got %f : expected %f)".format(testNum, i, j, a, b))
          }
        }
      }
    }
  }
}


