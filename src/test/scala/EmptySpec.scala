
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import utils.FixedPointConversion.fixedToFloat

class EmptySpec extends AnyFreeSpec with ChiselScalatestTester {
  val bob = fixedToFloat(254, 0, 16, true)
  println(bob)
}
