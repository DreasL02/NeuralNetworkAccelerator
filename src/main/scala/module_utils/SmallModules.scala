package module_utils

import chisel3._
import chisel3.util.{Fill, log2Ceil}

object SmallModules {

  def timer(max: Int, reset: Bool): Bool = { // timer that counts up to max and stays there until reset manually by asserting reset
    val x = RegInit(0.U(log2Ceil(max + 1).W))
    val done = x === max.U // done when x reaches max
    x := Mux(reset, 0.U, Mux(done, x, x + 1.U)) // reset when reset is asserted, otherwise increment if not done
    done
  }

  def timer(max: Int, reset: Bool, enable: Bool): Bool = { // timer that counts up to max and stays there until reset manually by asserting reset
    val x = RegInit(0.U(log2Ceil(max + 1).W))
    val done = x === max.U // done when x reaches max
    x := Mux(reset, 0.U, Mux(done, x, Mux(enable, x + 1.U, x)))
    done
  }

  def mult(a: UInt, b: UInt, wOperands: Int, wResult: Int, signed: Boolean): UInt = {
    assert(wResult >= wOperands * 2)
    val multiplicationOperation = Wire(UInt(wResult.W))
    if (signed) {
      val signedMul = Wire(UInt((2 * wOperands).W))
      signedMul := (a.asSInt * b.asSInt).asUInt
      if (wResult > 2 * wOperands) {
        val signExtension = Fill(wResult - (2 * wOperands), signedMul(2 * wOperands - 1))
        multiplicationOperation := signExtension ## signedMul // sign extend to wResult
      } else {
        multiplicationOperation := signedMul
      }
    } else {
      multiplicationOperation := a * b
    }
    multiplicationOperation
  }
}
