package operators

import chisel3._
import chisel3.util.DecoupledIO
import module_utils.{CalculationDelayInterfaceFSM, NoCalculationDelayInterfaceFSM}
import module_utils.SmallModules.timer

class ReLU(w: Int, numberOfRows: Int, numberOfColumns: Int, signed: Boolean) extends Module {

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W)))))

    val outputChannel = new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))
  })

  val result = Wire(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))

  for (row <- 0 until numberOfRows) {
    for (column <- 0 until numberOfColumns) {
      result(row)(column) := io.inputChannel.bits(row)(column) //default is the same value

      if (signed) { //if the values are signed
        when(io.inputChannel.bits(row)(column) >> (w - 1).U === 1.U) { //if signed bit (@msb) is 1, the result is negative
          result(row)(column) := 0.U //ReLU gives 0
        }
      }
    }
  }

  private val interfaceFSM = Module(new NoCalculationDelayInterfaceFSM)
  interfaceFSM.io.inputValid := io.inputChannel.valid
  interfaceFSM.io.outputReady := io.outputChannel.ready

  io.outputChannel.valid := interfaceFSM.io.outputValid
  io.inputChannel.ready := interfaceFSM.io.inputReady

  val buffer = RegInit(VecInit.fill(numberOfRows, numberOfColumns)(0.U(w.W)))
  when(interfaceFSM.io.storeResult) {
    buffer := result
  }
  io.outputChannel.bits := buffer
}
