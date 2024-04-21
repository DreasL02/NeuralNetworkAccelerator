package operators

import chisel3._
import chisel3.util._
import module_utils.InterfaceFSM
import module_utils.SmallModules.timer

class Rounder(
               wBefore: Int = 8,
               wAfter: Int = 16,
               numberOfRows: Int = 4,
               numberOfColumns: Int = 4,
               signed: Boolean = true,
               fixedPoint: Int = 0
             ) extends Module {

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(wBefore.W)))))

    val outputChannel = new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(wAfter.W))))
  })

  val results = Wire(Vec(numberOfRows, Vec(numberOfColumns, UInt(wAfter.W))))
  for (row <- 0 until numberOfRows) {
    for (column <- 0 until numberOfColumns) {
      if (signed) {
        val sign = io.inputChannel.bits(row)(column)(wBefore - 1)
        if (fixedPoint == 0) {
          results(row)(column) := sign ## io.inputChannel.bits(row)(column)(wBefore - 1, 0)
        } else {
          results(row)(column) := sign ## ((io.inputChannel.bits(row)(column) + (1.U << (fixedPoint.U - 1.U)).asUInt) >> fixedPoint.U)(wBefore - 1, 0).asUInt
        }
      } else {
        if (fixedPoint == 0) {
          results(row)(column) := io.inputChannel.bits(row)(column)
        } else {
          results(row)(column) := ((io.inputChannel.bits(row)(column) + (1.U << (fixedPoint.U - 1.U)).asUInt) >> fixedPoint.U).asUInt
        }
      }
    }
  }

  private val cyclesUntilOutputValid: Int = 0
  private val interfaceFSM = Module(new InterfaceFSM)
  interfaceFSM.io.inputValid := io.inputChannel.valid
  interfaceFSM.io.outputReady := io.outputChannel.ready
  interfaceFSM.io.doneWithCalculation := timer(cyclesUntilOutputValid, interfaceFSM.io.calculateStart)

  io.outputChannel.valid := interfaceFSM.io.outputValid
  io.inputChannel.ready := interfaceFSM.io.inputReady

  val buffer = RegInit(VecInit.fill(numberOfRows, numberOfColumns)(0.U(wAfter.W)))
  when(interfaceFSM.io.storeResult) {
    buffer := results
  }
  io.outputChannel.bits := buffer
}
