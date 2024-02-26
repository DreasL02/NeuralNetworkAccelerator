import chisel3._

import module_utils.Adders
import scala_utils.Optional.optional


// Describes a addition operation of two matrices, with the second matrix being a bias matrix that is continuously
// emitted until assertion of the load signal

class BufferedBias(w: Int = 8, numberOfRows: Int = 4, numberOfColumns: Int = 4, enableDebuggingIO: Boolean = true) extends Module {
  val io = IO(new Bundle {
    val input = Input(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))
    val biases = Input(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))
    val load = Input(Bool())

    val result = Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))

    val debugBiases = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W)))))
  })

  val adders = Module(new Adders(w, numberOfRows, numberOfColumns))
  adders.io.operandA := io.input

  for (row <- 0 until numberOfRows) {
    for (column <- 0 until numberOfColumns) {
      // Continuously emit bias values
      val biasReg = RegInit(0.U(w.W))
      when(io.load) {
        biasReg := io.biases(row)(column) // replace bias value
      }

      adders.io.operandB(row)(column) := biasReg
      if (enableDebuggingIO) {
        io.debugBiases.get(row)(column) := biasReg
      }
    }
  }

  io.result := adders.io.result
}
