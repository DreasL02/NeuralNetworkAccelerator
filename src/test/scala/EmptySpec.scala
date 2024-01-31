import Utils._
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
class EmptySpec extends AnyFreeSpec with ChiselScalatestTester {
  val bob = FileReader.readMatrixFromFile("src/test/scala/Utils/test.txt")
  for (i <- bob.indices) {
    for (j <- bob(0).indices) {
      print(bob(i)(j).toString() + " ")
    }
    println()
  }
}
