import chisel3.emitVerilog
import systolic_array.SystolicArray


object Main extends App {
  emitVerilog(new MatrixMultiplicationUnit(16, 4))
}