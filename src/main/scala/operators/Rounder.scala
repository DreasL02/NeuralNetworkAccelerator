package operators

import chisel3._
import chisel3.util._
import module_utils.{CalculationDelayInterfaceFSM, NoCalculationDelayInterfaceFSM}
import module_utils.SmallModules.timer

class Rounder(
               wBefore: Int,
               wAfter: Int,
               numberOfRows: Int,
               numberOfColumns: Int,
               signed: Boolean,
               fixedPoint: Int
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
        val maxPositiveValue = ((1 << (wAfter - 1)) - 1).U
        val minNegativeValue = (1 << (wAfter - 1)).U
        println("maxPositiveValue: " + maxPositiveValue)
        println("minNegativeValue: " + minNegativeValue)
        // saturation check
        when(io.inputChannel.bits(row)(column) > minNegativeValue) {
          results(row)(column) := minNegativeValue
        }.elsewhen(io.inputChannel.bits(row)(column) > maxPositiveValue) {
          results(row)(column) := maxPositiveValue
        }.otherwise( // no saturation
          if (fixedPoint == 0) {
            results(row)(column) := sign ## io.inputChannel.bits(row)(column)(wBefore - 1, 0)
          } else {
            results(row)(column) := sign ## ((io.inputChannel.bits(row)(column) + (1.U << (fixedPoint.U - 1.U)).asUInt) >> fixedPoint.U)(wBefore - 1, 0).asUInt
          }
        )
      } else {
        // saturation check
        val maxPositiveValue = (1 << wAfter).U - 1.U
        when(io.inputChannel.bits(row)(column) > maxPositiveValue) {
          results(row)(column) := maxPositiveValue
        }.otherwise( // no saturation
          if (fixedPoint == 0) {
            results(row)(column) := io.inputChannel.bits(row)(column)
          } else {
            results(row)(column) := ((io.inputChannel.bits(row)(column) + (1.U << (fixedPoint.U - 1.U)).asUInt) >> fixedPoint.U).asUInt
          }
        )
      }
    }
  }


  private val interfaceFSM = Module(new NoCalculationDelayInterfaceFSM)
  interfaceFSM.io.inputValid := io.inputChannel.valid
  interfaceFSM.io.outputReady := io.outputChannel.ready

  io.outputChannel.valid := interfaceFSM.io.outputValid
  io.inputChannel.ready := interfaceFSM.io.inputReady

  val buffer = RegInit(VecInit.fill(numberOfRows, numberOfColumns)(0.U(wAfter.W)))
  when(interfaceFSM.io.storeResult) {
    buffer := results
  }
  io.outputChannel.bits := buffer
}
