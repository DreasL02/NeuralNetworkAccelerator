import chisel3.emitVerilog

object Main extends App {

  val filepath = "ONNX Python/json/conv.json"

  val lists = SpecToListConverter.convertSpecToLists(filepath)

  val pipelineIO = false

  emitVerilog(new AutomaticGeneration(lists._2, lists._3, pipelineIO, true, false))

}