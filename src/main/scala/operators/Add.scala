package operators

import chisel3._
import chisel3.util.DecoupledIO
import module_utils.NoCalculationDelayInterfaceFSM
import module_utils.SmallModules.timer
import scala_utils.Optional.optional

class Add(w: Int, numberOfRows: Int, numberOfColumns: Int, enableDebuggingIO: Boolean) extends Module {
  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W)))))
    val biasChannel = Flipped(new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W)))))

    val outputChannel = new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))

    val debugBiases = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W)))))
  })

  val result = Wire(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))
  // adds the values and biases together
  for (row <- 0 until numberOfRows) {
    for (column <- 0 until numberOfColumns) {
      result(row)(column) := io.inputChannel.bits(row)(column) + io.biasChannel.bits(row)(column)
    }
  }

  if (enableDebuggingIO) {
    io.debugBiases.get := io.biasChannel.bits
  }

  private val interfaceFSM = Module(new NoCalculationDelayInterfaceFSM)
  interfaceFSM.io.inputValid := io.inputChannel.valid && io.biasChannel.valid
  interfaceFSM.io.outputReady := io.outputChannel.ready

  io.outputChannel.valid := interfaceFSM.io.outputValid
  io.inputChannel.ready := interfaceFSM.io.inputReady
  io.biasChannel.ready := interfaceFSM.io.inputReady

  val buffer = RegInit(VecInit.fill(numberOfRows, numberOfColumns)(0.U(w.W)))
  when(interfaceFSM.io.storeResult) {
    buffer := result
  }
  io.outputChannel.bits := buffer
}
