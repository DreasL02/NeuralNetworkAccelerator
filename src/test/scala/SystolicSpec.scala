import Utils.MatrixUtils._
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import systolic_array.SystolicArray

//m = y-axis
//m(0) = x-axis


class SystolicSpec extends AnyFreeSpec with ChiselScalatestTester {
  val enablePrintingInFirstTest = false
  "SystolicArray should calculate a 3x3 * 3x3 matrix multiplication with fixed point at 3 correctly" in {
    test(new SystolicArray(w = 16, dimension = 3)) { dut =>
      var m1f = Array(Array(1.2f, 1.3f, 2.4f), Array(0.9f, 3.4f, 0.9f), Array(2.2f, 1.2f, 0.9f))
      var m2f = Array(Array(2.2f, 1.3f, 1.5f), Array(4.9f, 0.4f, 8.8f), Array(2.2f, 1.2f, 0.9f))

      val fixedPoint = 3

      var mrf = calculateMatrixMultiplication(m1f, m2f)
      if (enablePrintingInFirstTest)
        printMatrixMultiplication(m1f, m2f, mrf, "GOLDEN MODEL CALCULATION IN PURE FLOATING")

      val m1 = convertFloatMatrixToFixedMatrix(m1f, fixedPoint)
      val m2 = convertFloatMatrixToFixedMatrix(m2f, fixedPoint)
      val mr = calculateMatrixMultiplication(m1, m2)

      if (enablePrintingInFirstTest)
        printMatrixMultiplication(m1, m2, mr, "GOLDEN MODEL CALCULATION IN FIXED POINT")

      m1f = convertFixedMatrixToFloatMatrix(m1, fixedPoint)
      m2f = convertFixedMatrixToFloatMatrix(m2, fixedPoint)
      mrf = calculateMatrixMultiplication(m1f, m2f) // TODO: not rounded up when fixed point
      if (enablePrintingInFirstTest)
        printMatrixMultiplication(m1f, m2f, mrf, "GOLDEN MODEL CALCULATION IN AFTER TRANSFORMATION BACK TO FLOATING")

      val mm1 = convertMatrixToMappedAMatrix(m1)
      val mm2 = convertMatrixToMappedBMatrix(m2)

      dut.io.fixedPoint.poke(fixedPoint.asUInt)

      //print(matrixToString(mm1))
      //print(matrixToString(mm2))

      val max_number_of_cycles = mm1.length * mm2(0).length
      for (cycle <- 0 until max_number_of_cycles) {
        for (i <- mm1.indices) {
          //println("c: %d i: %d v: %d".format(cycle, i, mm1(i)(mm1(0).length - 1 - cycle)))
          dut.io.a(i).poke(mm1(i)(mm1(0).length - 1 - cycle))
        }
        //println("----")
        for (i <- mm2(0).indices) {
          //println("c: %d i: %d v: %d".format(cycle, i, mm2(mm2.length - 1 - cycle)(i)))
          dut.io.b(i).poke(mm2(mm2.length - 1 - cycle)(i))
        }
        //println("****")
        dut.clock.step()
      }

      val resultFixed = Array.fill(mm1.length, mm2(0).length)(0)
      for (i <- mr.indices) {
        for (j <- mr(0).indices) {
          resultFixed(i)(j) = dut.io.c(j)(i).peek().litValue.toInt
        }
      }

      val resultFloat = convertFixedMatrixToFloatMatrix(resultFixed, fixedPoint)


      if (enablePrintingInFirstTest) {
        println("---- SYSTOLIC ARRAY RESULT IN FIXED ----")
        print(matrixToString(resultFixed))
        println("---- SYSTOLIC ARRAY RESULT IN FLOAT ----")
        print(matrixToString(resultFloat))
      }

      for (i <- mrf.indices) {
        for (j <- mrf(0).indices) {
          val a = resultFloat(i)(j)
          val b = mrf(i)(j)
          var valid = false
          if (a - 1 <= b && a + 1 >= b) { //within +-1 of golden model
            valid = true
          }
          assert(valid, ": element at (%d,%d) did not match (got %f : expected %f)".format(i, j, a, b))
        }
      }
    }
  }
  "SystolicArray should calculate a 3x3 * 3x3 matrix multiplication with fixed point at 5 correctly" in {
    test(new SystolicArray(w = 16, dimension = 3)) { dut =>
      var m1f = Array(Array(1.2f, 1.3f, 2.4f), Array(0.9f, 3.4f, 0.9f), Array(2.2f, 1.2f, 0.9f))
      var m2f = Array(Array(2.2f, 1.3f, 1.5f), Array(4.9f, 0.4f, 8.8f), Array(2.2f, 1.2f, 0.9f))
      val fixedPoint = 5

      var mrf = calculateMatrixMultiplication(m1f, m2f)
      val m1 = convertFloatMatrixToFixedMatrix(m1f, fixedPoint)
      val m2 = convertFloatMatrixToFixedMatrix(m2f, fixedPoint)
      val mr = calculateMatrixMultiplication(m1, m2)
      m1f = convertFixedMatrixToFloatMatrix(m1, fixedPoint)
      m2f = convertFixedMatrixToFloatMatrix(m2, fixedPoint)
      mrf = calculateMatrixMultiplication(m1f, m2f) // TODO: not rounded up when fixed point
      val mm1 = convertMatrixToMappedAMatrix(m1)
      val mm2 = convertMatrixToMappedBMatrix(m2)

      dut.io.fixedPoint.poke(fixedPoint.asUInt)


      val max_number_of_cycles = mm1.length * mm2(0).length
      for (cycle <- 0 until max_number_of_cycles) {
        for (i <- mm1.indices) {
          dut.io.a(i).poke(mm1(i)(mm1(0).length - 1 - cycle))
        }
        for (i <- mm2(0).indices) {
          dut.io.b(i).poke(mm2(mm2.length - 1 - cycle)(i))
        }
        dut.clock.step()
      }

      val resultFixed = Array.fill(mm1.length, mm2(0).length)(0)
      for (i <- mr.indices) {
        for (j <- mr(0).indices) {
          resultFixed(i)(j) = dut.io.c(j)(i).peek().litValue.toInt
        }
      }

      val resultFloat = convertFixedMatrixToFloatMatrix(resultFixed, fixedPoint)
      for (i <- mrf.indices) {
        for (j <- mrf(0).indices) {
          val a = resultFloat(i)(j)
          val b = mrf(i)(j)
          var valid = false
          if (a - 1 <= b && a + 1 >= b) { //within +-1 of golden model
            valid = true
          }
          assert(valid, ": element at (%d,%d) did not match (got %f : expected %f)".format(i, j, a, b))
        }
      }
    }
  }
}


