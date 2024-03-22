import chisel3.emitVerilog

object Main extends App {

  val filepath = "ONNX Python/example_spec_file.json"

  val lists: (List[Any], List[List[Int]]) = SpecToListConverter.convertSpecToLists(filepath)

  val pipelineIO = true

  emitVerilog(new AutomaticGeneration(lists._1, lists._2, pipelineIO, false, false))

}