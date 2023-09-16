package systolic_array

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._


class SystolicSpec extends AnyFreeSpec with ChiselScalatestTester{

  def calculateMatrixMultiplication(v_a: Array[Array[Int]], v_b : Array[Array[Int]]): Array[Array[Int]] = {
    val v_c : Array[Array[Int]] = Array.fill(v_a.length,v_b(0).length)(0)
    for(i <- v_a.indices) {
      for (j <- v_b.indices) {
        var sum = 0
        for (k <- v_b(0).indices) {
          sum = sum + v_a(i)(k)*v_b(k)(j)
        }
        v_c(i)(j) = sum
      }
    }
    v_c
  }


  "SystolicArray should calculate a 2x2 * 2x2 matrix multiplication correctly in 4 cycles" in {
    test(new SystolicArray(w = 16, horizontal = 2,vertical = 2)) { dut =>
      val values_in_a = Vector(2,2,3,3)
      val values_in_b = Vector(1,3,5,6)
      val values_in_c = Vector(12, 18, 18, 27)

      val a = Array.fill(2,4)(0)
      val b = Array.fill(2,4)(0)

      a(0)(0) = values_in_a(0)
      a(1)(0) = values_in_a(1)
      a(1)(1) = values_in_a(2)
      a(0)(2) = values_in_a(3)

      b(0)(0) = values_in_b(0)
      b(1)(0) = values_in_b(1)
      b(1)(1) = values_in_b(2)
      b(0)(2) = values_in_b(3)

      println("%d %d * %d %d = %d %d \n%d %d   %d %d   %d %d".format(
        values_in_a(0), values_in_a(1),
        values_in_b(0), values_in_b(1),
        values_in_c(0), values_in_c(1),
        values_in_a(2), values_in_a(3),
        values_in_b(2), values_in_b(3),
        values_in_c(2), values_in_c(3)
      ))
      println("CYCLE %d".format(0))
      println("%d %d \n%d %d".format(
        dut.io.c(0).peekInt(), dut.io.c(1).peekInt(),
        dut.io.c(2).peekInt(), dut.io.c(3).peekInt()))
      val cycles = 4
      for (i <- 0 until cycles){
        dut.io.a(0).poke(a(0)(i))
        dut.io.a(1).poke(a(1)(i))

        dut.io.b(0).poke(b(0)(i))
        dut.io.b(1).poke(b(1)(i))

        dut.clock.step()
        println("CYCLE %d".format(i+1))
        println("%d %d \n%d %d".format(
          dut.io.c(0).peekInt(), dut.io.c(1).peekInt(),
          dut.io.c(2).peekInt(), dut.io.c(3).peekInt()))
      }
      for (i <- 0 until values_in_c.length) {
        dut.io.c(i).expect(values_in_c(i))
      }
    }
  }
}
