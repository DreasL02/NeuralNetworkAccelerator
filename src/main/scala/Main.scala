import scala_utils.{FixedPointConversion, Mapping}
import chisel3.emitVerilog
import communication.chisel.lib.uart.UartTx
import systolic_array.SystolicArray

object Main extends App {

  val filepath = "ONNX Python/example_spec_file.json"

  val lists: (List[Any], List[List[Int]]) = SpecToListConverter.convertSpecToLists(filepath)

  emitVerilog(new AutomaticGeneration(lists._1, lists._2, false, false))
}


/*

object Main extends App {
  print("UART \n")
  emitVerilog(new UartTx(50000000 * 2, 115200))
}
*/
