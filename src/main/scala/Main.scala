import chisel3.emitVerilog

object Main extends App {

  val filepath = "ONNX Python/example_spec_file.json"

  val lists = SpecToListConverter.convertSpecToLists(filepath)

  val pipelineIO = true

  emitVerilog(new AutomaticGeneration(lists._2, lists._3, pipelineIO, false, false))

}