import chisel3.emitVerilog
import onnx.SpecToListConverter

object Main extends App {
  val filepath = "ONNX Python/json/sinus_open_q1.7.8_direct.json"

  val lists = SpecToListConverter.convertSpecToLists(filepath)

  val print = true

  emitVerilog(new AutomaticGeneration(lists._2, lists._3, print))
}
