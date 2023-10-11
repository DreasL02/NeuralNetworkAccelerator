import chisel3.emitVerilog
import systolic_array.SystolicArray
import org.emergentorder.onnx.{ONNXBytesDataSource, ONNXHelper}

import java.nio.file.{Files, Paths}

object Main extends App {
  emitVerilog(new SystolicArray())
}