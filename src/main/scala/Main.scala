import chisel3.emitVerilog
import onnx.SpecToListConverter

object Main extends App {
  val filepath = "ONNX Python/json/8x8_open.json"

  val lists = SpecToListConverter.convertSpecToLists(filepath)

  val print = true

  emitVerilog(new AutomaticGeneration(lists._2, lists._3, print))
}
