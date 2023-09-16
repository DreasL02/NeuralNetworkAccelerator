import chisel3.{emitVerilog, fromIntToWidth}
import systolic_array.{SystolicArray}
object Main extends App {
  emitVerilog(new SystolicArray())
}