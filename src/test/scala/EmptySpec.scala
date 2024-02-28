
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import scala_utils.FixedPointConversion.{fixedToFloat, floatToFixed}

class EmptySpec extends AnyFreeSpec with ChiselScalatestTester {

  println(floatToFixed(1.0f, 0, 32, signed = true))
  println(floatToFixed(0.0f, 0, 32, signed = true))
  println(floatToFixed(-1.0f, 0, 32, signed = true))
  println(floatToFixed(-2.0f, 0, 32, signed = true))
  println(floatToFixed(-3.0f, 0, 32, signed = true))
}
