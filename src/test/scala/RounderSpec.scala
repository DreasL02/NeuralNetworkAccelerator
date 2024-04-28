import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import operators.Rounder
import scala_utils.FixedPointConversion.{fixedToFloat, floatToFixed}


class RounderSpec extends AnyFreeSpec with ChiselScalatestTester {

  val wBefore = 4
  val wAfter = 3
  val fixedPointAfter = 0
  val fixedPointBefore = fixedPointAfter * 2
  val inputFloat = -7f
  val sign = true
  val inputFixed = floatToFixed(inputFloat, fixedPointBefore, wBefore, sign)

  "Rounder should round correctly" in {
    test(new Rounder(wBefore, wAfter, 1, 1, sign, fixedPointAfter)) {
      dut =>
        println(s"input: $inputFloat, inputFixed: $inputFixed")
        dut.io.inputChannel.valid.poke(true.B)
        dut.io.outputChannel.ready.poke(true.B)
        dut.io.inputChannel.bits(0)(0).poke(inputFixed.U)
        dut.clock.step(1)
        val outputFixed = dut.io.outputChannel.bits(0)(0).peek().litValue
        val outputFloat = fixedToFloat(outputFixed, fixedPointAfter, wAfter, sign)
        println(s"outputFixed: $outputFixed")
        println(s"input: $inputFloat, output: $outputFloat")
    }
  }
}
