import chisel3.emitVerilog

object Main extends App {
  val filepath = "ONNX Python/json/smaller_mnist.json"

  val lists = SpecToListConverter.convertSpecToLists(filepath)

  val pipelineIO = false

  val print = true

  emitVerilog(new AutomaticGeneration(lists._2, lists._3, pipelineIO, false, print))
}
