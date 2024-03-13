package activation_functions

import chisel3._
import chisel3.util.DecoupledIO

class ReLU(w: Int = 8, numberOfRows: Int = 4, numberOfColumns: Int = 4, signed: Boolean = true) extends Module {
  def this(reluType: onnx.Operators.ReLUType) = this(
    reluType.wOperands,
    reluType.operandDimensions._1,
    reluType.operandDimensions._2,
    reluType.signed
  )

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W)))))
    
    val resultChannel = new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))
  })

  for (row <- 0 until numberOfRows) {
    for (column <- 0 until numberOfColumns) {
      io.resultChannel.bits(row)(column) := io.inputChannel.bits(row)(column) //default is the same value

      if (signed) { //if the values are signed
        when(io.inputChannel.bits(row)(column) >> (w - 1).U === 1.U) { //if signed bit (@msb) is 1, the result is negative
          io.resultChannel.bits(row)(column) := 0.U //ReLU gives 0
        }
      }
    }
  }

  io.resultChannel.ready := io.inputChannel.ready // Output is ready as soon as input is ready
  io.inputChannel.valid := io.resultChannel.ready && io.resultChannel.valid // Ready to receive new inputs when the result channel is ready and valid
}
