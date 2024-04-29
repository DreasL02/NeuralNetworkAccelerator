import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import operators.{Rounder, Tanh}
import scala_utils.FixedPointConversion.{fixedToFloat, floatToFixed}


class TanhSpec extends AnyFreeSpec with ChiselScalatestTester {

  val w = 16
  val fixedPoint = 8
  val inputFloat = 0.6f
  val sign = true
  val inputFixed = floatToFixed(inputFloat, fixedPoint, w, sign)

  "Rounder should round correctly" in {
    test(new Tanh(w, (1, 1), fixedPoint, true)) {
      dut =>
        println(s"input: $inputFloat, inputFixed: $inputFixed")
        dut.io.inputChannel.valid.poke(true.B)
        dut.io.outputChannel.ready.poke(true.B)
        dut.io.inputChannel.bits(0)(0).poke(inputFixed.U)
        dut.clock.step(1)
        val outputFixed = dut.io.outputChannel.bits(0)(0).peek().litValue
        val outputFloat = fixedToFloat(outputFixed, fixedPoint, w, sign)
        println(s"outputFixed: $outputFixed")
        println(s"input: $inputFloat, output: $outputFloat")
        println(s"expected: ${Math.tanh(inputFloat)}")
    }
  }
}
