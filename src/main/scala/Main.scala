import chisel3.emitVerilog
import onnx.SpecToListConverter

object Main extends App {
  val filepath = "ONNX Python/json/8x8_uart.json"

  val lists = SpecToListConverter.convertSpecToLists(filepath)

  val print = true

  emitVerilog(new AutomaticGeneration(lists._2, lists._3, print))
}
