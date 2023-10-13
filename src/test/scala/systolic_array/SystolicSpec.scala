package systolic_array

import Utils.FixedPointConverter
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._
//m = y-axis
//m(0) = x-axis

class SystolicSpec extends AnyFreeSpec with ChiselScalatestTester {

  def calculateMatrixMultiplication(m1: Array[Array[Float]], m2: Array[Array[Float]]): Array[Array[Float]] = {
    //https://en.wikipedia.org/wiki/Matrix_multiplication_algorithm
    val mr: Array[Array[Float]] = Array.fill(m1.length, m2(0).length)(0)
    for (i <- m1.indices) {
      for (j <- m2.indices) {
        var sum = 0f
        for (k <- m2(0).indices) {
          sum = sum + m1(i)(k) * m2(k)(j)
        }
        mr(i)(j) = sum
      }
    }
    mr
  }

  def calculateMatrixMultiplication(m1: Array[Array[Int]], m2: Array[Array[Int]]): Array[Array[Int]] = {
    //https://en.wikipedia.org/wiki/Matrix_multiplication_algorithm
    val mr: Array[Array[Int]] = Array.fill(m1.length, m2(0).length)(0)
    for (i <- m1.indices) {
      for (j <- m2.indices) {
        var sum = 0
        for (k <- m2(0).indices) {
          sum = sum + m1(i)(k) * m2(k)(j)
        }
        mr(i)(j) = sum
      }
    }
    mr
  }

  def convertFloatMatrixToFixedMatrix(mf: Array[Array[Float]], fixedPointFractionBits: Int): Array[Array[Int]] = {
    val m: Array[Array[Int]] = Array.fill(mf.length, mf(0).length)(0)
    for (i <- mf.indices) {
      for (j <- mf(0).indices) {
        m(i)(j) = FixedPointConverter.floatToFixed(mf(i)(j), fixedPointFractionBits).toInt
      }
    }
    m
  }

  def convertFixedMatrixToFloatMatrix(m: Array[Array[Int]], fixedPointFractionBits: Int): Array[Array[Float]] = {
    val mf: Array[Array[Float]] = Array.fill(m.length, m(0).length)(0)
    for (i <- m.indices) {
      for (j <- m(0).indices) {
        mf(i)(j) = FixedPointConverter.fixedToFloat(m(i)(j), fixedPointFractionBits)
      }
    }
    mf
  }

  def convertMatrixToMappedAMatrix(m: Array[Array[Int]]): Array[Array[Int]] = {
    val m_a: Array[Array[Int]] = Array.fill(m(0).length, m.length * m.length)(0)
    for (i <- m.indices) {
      for (j <- m(0).indices) {
        m_a(i)(m.length * m.length - 1 - i - j) = m(i)(m.length - 1 - j)
      }
    }
    m_a
  }

  def convertMatrixToMappedBMatrix(m: Array[Array[Int]]): Array[Array[Int]] = {
    val m_b: Array[Array[Int]] = Array.fill(m(0).length * m(0).length, m.length)(0)
    for (i <- m.indices) {
      for (j <- m(0).indices) {
        m_b(m.length * m.length - 1 - i - j)(j) = m(m.length - 1 - i)(j)
      }
    }
    m_b
  }

  def matrixToString(m: Array[Array[Float]]): String = {
    var str = ""
    for (i <- m.indices) {
      for (j <- m(0).indices) {
        str = str + "%f ".format(m(i)(j))
      }
      str = str + "\n"
    }
    str
  }

  def matrixToString(m: Array[Array[Int]]): String = {
    var str = ""
    for (i <- m.indices) {
      for (j <- m(0).indices) {
        str = str + "%d ".format(m(i)(j))
      }
      str = str + "\n"
    }
    str
  }

  "SystolicArray should calculate a 3x3 * 3x3 matrix multiplication correctly" in {
    test(new SystolicArray(w = 16, dimension = 3)) { dut =>
      var m1f = Array(Array(1.2f, 1.3f, 2.4f), Array(0.9f, 3.4f, 0.9f), Array(2.2f, 31.2f, 0.9f))
      var m2f = Array(Array(2.2f, 1.3f, 10.0f), Array(4.9f, 0.4f, 8.8f), Array(2.2f, 1.2f, 0.9f))

      val fixedPoint = 4

      var mrf = calculateMatrixMultiplication(m1f, m2f)

      print(matrixToString(m1f))
      println("*")
      print(matrixToString(m2f))
      println("=")
      print(matrixToString(mrf))


      val m1 = convertFloatMatrixToFixedMatrix(m1f, fixedPoint)
      val m2 = convertFloatMatrixToFixedMatrix(m2f, fixedPoint)
      val mr = calculateMatrixMultiplication(m1, m2)

      println("-------")
      print(matrixToString(m1))
      println("*")
      print(matrixToString(m2))
      println("=")
      print(matrixToString(mr))

      m1f = convertFixedMatrixToFloatMatrix(m1, fixedPoint)
      m2f = convertFixedMatrixToFloatMatrix(m2, fixedPoint)
      mrf = calculateMatrixMultiplication(m1f, m2f)

      print(matrixToString(m1f))
      println("*")
      print(matrixToString(m2f))
      println("=")
      print(matrixToString(mrf))

      val mm1 = convertMatrixToMappedAMatrix(m1)
      val mm2 = convertMatrixToMappedBMatrix(m2)

      dut.io.fixedPoint.poke(fixedPoint.asUInt)
      //print(matrixToString(mm1))
      //print(matrixToString(mm2))

      val max_number_of_cycles = mm1.length * mm2(0).length
      for (cycle <- 0 until max_number_of_cycles) {
        for (i <- mm1.indices) {
          //println("c: %d i: %d v: %d".format(cycle, i, mm1(i)(mm1(0).length-1-cycle)))
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

      val result = Array.fill(mm1.length, mm2(0).length)(0)
      for (i <- mr.indices) {
        for (j <- mr(0).indices) {
          result(i)(j) = dut.io.c(j)(i).peek().litValue.toInt
        }
      }
      val result_fixed = convertFixedMatrixToFloatMatrix(result, fixedPoint)
      print(matrixToString(result_fixed))
      print(matrixToString(result))

      for (i <- mrf.indices) {
        for (j <- mrf(0).indices) {
          val a = result_fixed(i)(j)
          val b = mrf(i)(j)
          var valid = false
          if (a - 1 <= b && a + 1 >= b) {
            valid = true
          }
          assert(valid, ": element at (%d,%d) did not match (got %f : expected %f)".format(i, j, a, b))
        }
      }
    }
  }
}
