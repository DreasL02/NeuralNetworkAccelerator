
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class AutomaticGenerationSpec extends AnyFreeSpec with ChiselScalatestTester {
  val lists: (List[Any], List[List[Int]]) = SpecToListConverter.convertSpecToLists("src/main/scala/scala_utils/data/example_spec_file.json")

  // Print the lists
  println(lists._1)
  println(lists._2)

  "AutomaticGenerationSpec should behave correctly" in {
    test(new AutomaticGeneration(lists._1, lists._2)) { dut =>
      dut.clock.step()
    }
  }
}