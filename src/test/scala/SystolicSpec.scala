
import chisel3._
import chisel3.util.Fill
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import systolic_array.SystolicArray
import scala_utils.RandomData._
import scala_utils.MatrixUtils._
import scala_utils.FixedPointConversion._

class SystolicSpec extends AnyFreeSpec with ChiselScalatestTester {
  // ======= configure the test =======
  val w = 7
  val wResult = 4 * w
  val numberOfColumns = 7 // number of columns in the result matrix
  val numberOfRows = 5 // number of rows in the result matrix
  val matrixCommonDimension = 10 // number of columns in the first matrix and number of rows in the second matrix
  val fixedPoint = 3
  val signed = true
  val numberOfTests = 10
  val max = 3.2f
  val min = -3.2f
  val threshold = 0.1f
  val printing = Array.fill(numberOfTests)(false)

  // We can enable printing for a specific test by setting the index to true
  printing(0) = true



  // ======= configure the test end =======

  // --- Rest should be left as is ---
  val seeds = Array.fill(numberOfTests * 2)(0)
  // increment seeds for each test and matrix to get different random numbers
  for (i <- 0 until numberOfTests * 2) {
    seeds(i) = i
  }

  // for each seed, generate a random matrix and test
  for (testNum <- 0 until numberOfTests) {
    val enablePrinting = printing(testNum)
    "SystolicArray should calculate correctly for test %d".format(testNum) in {
      test(new SystolicArray(w, wResult, numberOfRows, numberOfColumns, signed)) { dut =>
        var m1f = randomMatrix(numberOfRows, matrixCommonDimension, min, max, seeds(testNum * 2))
        var m2f = randomMatrix(matrixCommonDimension, numberOfColumns, min, max, seeds(testNum * 2 + 1))

        var mrf = calculateMatrixMultiplication(m1f, m2f)
        if (enablePrinting)
          printMatrixMultiplication(m1f, m2f, mrf, "GOLDEN MODEL CALCULATION IN PURE FLOATING")

        val m1 = convertFloatMatrixToFixedMatrix(m1f, fixedPoint, w, signed)
        val m2 = convertFloatMatrixToFixedMatrix(m2f, fixedPoint, w, signed)
        val mr = calculateMatrixMultiplication(m1, m2)

        val ms1 = convertFloatMatrixToFixedMatrix(m1f, fixedPoint, w, signed)
        val ms2 = convertFloatMatrixToFixedMatrix(m2f, fixedPoint, w, signed)

        if (enablePrinting)
          printMatrixMultiplication(m1, m2, mr, "GOLDEN MODEL CALCULATION IN FIXED POINT")

        m1f = convertFixedMatrixToFloatMatrix(m1, fixedPoint, w, signed)
        m2f = convertFixedMatrixToFloatMatrix(m2, fixedPoint, w, signed)
        mrf = calculateMatrixMultiplication(m1f, m2f)
        if (enablePrinting) {
          printMatrixMultiplication(m1f, m2f, mrf, "GOLDEN MODEL CALCULATION IN AFTER TRANSFORMATION BACK TO FLOATING")

          println("-- Dimensions of matrices --")
          println("a: (%d,%d)".format(m1.length, m1(0).length))
          println("b: (%d,%d)".format(m2.length, m2(0).length))
        }


        val mm1 = convertMatrixToMappedAMatrix(ms1, numberOfColumns + matrixCommonDimension - 2)
        val mm2 = convertMatrixToMappedBMatrix(ms2, numberOfRows + matrixCommonDimension - 2)

        if (enablePrinting) {
          println("-- a --")
          print(matrixToString(mm1))
          println("-- b --")
          print(matrixToString(mm2))
          println("-- Dimensions --")
          println("m_a: (%d,%d)".format(mm1.length, mm1(0).length))
          println("m_b: (%d,%d)".format(mm2.length, mm2(0).length))
          println("---------")
        }

        val max_number_of_cycles = mm1(0).length
        if (enablePrinting) {
          println("Max number of cycles: %d".format(max_number_of_cycles))
        }
        for (cycle <- 0 until max_number_of_cycles) {
          var values = "a : "
          for (i <- mm1.indices) {
            values += mm1(i)(mm1(0).length - 1 - cycle).toString + ", "
            dut.io.a(i).poke(mm1(i)(mm1(0).length - 1 - cycle))
          }
          values += "\nb : "
          for (i <- mm2(0).indices) {
            values += mm2(mm2.length - 1 - cycle)(i).toString + ", "
            dut.io.b(i).poke(mm2(mm2.length - 1 - cycle)(i))
          }
          dut.clock.step()

          val resultFixed: Array[Array[BigInt]] = Array.fill(mm1.length, mm2(0).length)(0)
          for (i <- mr.indices) {
            for (j <- mr(0).indices) {
              resultFixed(i)(j) = dut.io.c(i)(j).peek().litValue
            }
          }
          if (enablePrinting) {
            println("Cycle %d:".format(cycle))
            println(values)
            print(matrixToString(resultFixed))
          }
        }

        val resultFixed: Array[Array[BigInt]] = Array.fill(mm1.length, mm2(0).length)(0)
        for (i <- mr.indices) {
          for (j <- mr(0).indices) {
            resultFixed(i)(j) = dut.io.c(i)(j).peek().litValue
          }
        }

        // width and fixed point are doubled because the result is the sum of two fixed point numbers and no rounding is done
        val resultFloat = convertFixedMatrixToFloatMatrix(resultFixed, fixedPoint * 2, w * 2, signed)


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


