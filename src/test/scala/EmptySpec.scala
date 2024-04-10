
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class EmptySpec extends AnyFreeSpec with ChiselScalatestTester {
  val filepath = "ONNX Python/json/mnist12.json"

  SpecToListConverter.convertSpecToLists(filepath)
}
