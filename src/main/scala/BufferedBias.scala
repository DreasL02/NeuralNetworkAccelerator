import chisel3._

import module_utils.Adders
import scala_utils.Optional.optional


// TODO: either fold into Adders or change functionality to be very inline with ONNX Add

class BufferedBias(w: Int = 8, numberOfRows: Int = 4, numberOfColumns: Int = 4, enableDebuggingIO: Boolean = true) extends Module {
  val io = IO(new Bundle {
    val input = Input(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))
    val biases = Input(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))

    val result = Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))

    val debugBiases = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W)))))

    val valid = Output(Bool()) // indicates that the module should be done
    val ready = Input(Bool()) // indicates that the module is ready to receive new inputs
  })

  val adders = Module(new Adders(w, numberOfRows, numberOfColumns))
  adders.io.operandA := io.input
  adders.io.operandB := io.biases
  io.result := adders.io.result

  if (enableDebuggingIO) {
    io.debugBiases.get := io.biases
  }

  io.valid := io.ready
}
