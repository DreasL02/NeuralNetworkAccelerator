package systolic_array

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._


class SystolicSpec extends AnyFreeSpec with ChiselScalatestTester{

  def calculateMatrixMultiplication(m1: Array[Array[Int]], m2 : Array[Array[Int]]): Array[Array[Int]] = {
    //https://en.wikipedia.org/wiki/Matrix_multiplication_algorithm
    val mr : Array[Array[Int]] = Array.fill(m1.length, m2(0).length)(0)
    for(i <- m1.indices) {
      for (j <- m2.indices) {
        var sum = 0
        for (k <- m2(0).indices) {
          sum = sum + m1(i)(k)*m2(k)(j)
        }
        mr(i)(j) = sum
      }
    }
    mr
  }

  def convertMatrixToMappedAMatrix(m: Array[Array[Int]]): Array[Array[Int]] = {
    val m_a : Array[Array[Int]] = Array.fill(m(0).length, 2 * m.length)(0)
    for (i <- m.indices) {
      for (j <- m(0).indices) {
        m_a(i)(j+(m.length-i)) = m(i)(j)
      }
    }
    m_a
  }
  def convertMatrixToMappedBMatrix(m : Array[Array[Int]]): Array[Array[Int]] = {
    val m_b: Array[Array[Int]] = Array.fill(2 * m(0).length, m.length)(0)
    for(i <- m.indices){
      for (j <- m(0).indices) {
        m_b(i+(m(0).length-j))(j) = m(i)(j)
      }
    }
    m_b
  }

  def matrixToString(m :Array[Array[Int]]) : String ={
    var str = ""
    for (i <- m.indices){
      for(j <- m(0).indices){
        str = str + "%d ".format(m(i)(j))
      }
      str = str + "\n"
    }
    str
  }

  "SystolicArray should calculate a 2x2 * 2x2 matrix multiplication correctly in 4 cycles" in {
    test(new SystolicArray(w = 16, horizontal = 2,vertical = 2)) { dut =>
      val m1 = Array(Array(2,3), Array(4,5))
      val m2 = Array(Array(1,3), Array(5,6))
      val mr = calculateMatrixMultiplication(m1, m2)
      /*
      print(matrixToString(m1))
      println("*")
      print(matrixToString(m2))
      println("=")
      print(matrixToString(mr))
      */
      val mm1 = convertMatrixToMappedAMatrix(m1)
      val mm2 = convertMatrixToMappedBMatrix(m2)

      //print(matrixToString(mm1))
      //print(matrixToString(mm2))

      val max_number_of_cycles = m1.length*m2(0).length
      for (cycle <- 0 until max_number_of_cycles){
        for (i <- mm1.indices){
          //println("i: %d j: %d v: %d".format(cycle, i, mm1(i)(mm1(0).length-1-cycle)))
          dut.io.a(i).poke(mm1(i)(mm1(0).length-1-cycle))
        }
        //println("----")
        for (i <- mm2(0).indices) {
          //println("i: %d j: %d v: %d".format(cycle, i, mm2(mm2.length - 1 - cycle)(i)))
          dut.io.b(i).poke(mm2(mm2.length - 1 - cycle)(i))
        }
        //println("****")
        dut.clock.step()
      }

      for (i <- mr.indices) {
        for (j <- mr(0).indices){
          dut.io.c(j)(i).expect(mr(i)(j))
        }
      }
    }
  }
}
