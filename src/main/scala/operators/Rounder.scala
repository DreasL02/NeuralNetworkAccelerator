package operators

import chisel3._
import chisel3.util._
import module_utils.{CalculationDelayInterfaceFSM, NoCalculationDelayInterfaceFSM}
import module_utils.SmallModules.timer
import scala_utils.Optional.optional

class Rounder(
               wBefore: Int,
               wAfter: Int,
               numberOfRows: Int,
               numberOfColumns: Int,
               signed: Boolean,
               fixedPoint: Int,
             ) extends Module {

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(wBefore.W)))))

    val outputChannel = new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(wAfter.W))))
  })

  val results = Wire(Vec(numberOfRows, Vec(numberOfColumns, UInt(wAfter.W))))
  for (row <- 0 until numberOfRows) {
    for (column <- 0 until numberOfColumns) {
      val roundedValue = Wire(UInt((wBefore - fixedPoint - 1).W)) // rounded value after moving the fixed point

      if (fixedPoint == 0) { // no fixed point, no rounding
        roundedValue := io.inputChannel.bits(row)(column).asUInt(wBefore - 1, 0)
      } else { // round to nearest
        roundedValue := ((io.inputChannel.bits(row)(column) + (1.U << (fixedPoint.U - 1.U)).asUInt) >> fixedPoint.U)
      }

      if (signed) {
        val sign = io.inputChannel.bits(row)(column)(wBefore - 1)
        val maxPositiveValue = ((1 << (wAfter - 1)) - 1).U
        val minNegativeValueNotSignExtended = 1 << (wAfter - 1)
        val minNegativeValue = Fill(wBefore - fixedPoint - 1 - wAfter, sign) ## minNegativeValueNotSignExtended.U

        when(sign === 1.U && roundedValue.asSInt < minNegativeValue.asSInt) { // saturation check in negative direction
          results(row)(column) := minNegativeValue
        }.elsewhen(sign === 0.U && roundedValue > maxPositiveValue) { // saturation check in positive direction
          results(row)(column) := maxPositiveValue
        }.otherwise { // no saturation
          results(row)(column) := sign ## roundedValue(wAfter - 2, 0).asUInt // move sign bit down
        }
      } else {
        // saturation check
        val maxPositiveValue = ((1 << wAfter) - 1).U

        when(roundedValue > maxPositiveValue) { // saturated
          results(row)(column) := maxPositiveValue
        }.otherwise( // no saturation
          results(row)(column) := roundedValue(wAfter - 1, 0).asUInt
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
