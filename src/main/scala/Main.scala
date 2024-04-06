import chisel3.emitVerilog

object Main extends App {

  val filepath = "ONNX Python/json/mnist12.json"

  val lists = SpecToListConverter.convertSpecToLists(filepath)

  val pipelineIO = true

  emitVerilog(new AutomaticGeneration(lists._2, lists._3, pipelineIO, false, false))

}