package operators

import chisel3._
import chisel3.util._
import module_utils.NoCalculationDelayInterfaceFSM
import scala_utils.FixedPointConversion._
import scala_utils.Optional.optional


class Tanh(
            w: Int,
            shape: (Int, Int),
            fixedPoint: Int,
            signed: Boolean,
          ) extends Module {

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(shape._1, Vec(shape._2, UInt(w.W)))))
    val outputChannel = new DecoupledIO(Vec(shape._1, Vec(shape._2, UInt(w.W))))
  })

  assert(signed == true, "Tanh only supports signed values")

  val result = Wire(Vec(shape._1, Vec(shape._2, UInt(w.W))))
  for (i <- 0 until shape._1) {
    for (j <- 0 until shape._2) {
      val max = floatToFixed(1f, fixedPoint, w, signed)
      val min = floatToFixed(-1f, fixedPoint, w, signed)

      val valAtMax = floatToFixed(1f, fixedPoint, w, signed)
      val valAtMin = floatToFixed(-1f, fixedPoint, w, signed)

      /*
      // implementation as in https://hal.science/hal-01654697/document
      val halfWayPos = floatToFixed(0.5f, fixedPoint, w, signed)
      val halfWayNeg = floatToFixed(-0.5f, fixedPoint, w, signed)

      when(io.inputChannel.bits(i)(j).asSInt > max.asSInt(w.W)) {
        result(i)(j) := valAtMax.U
      }.elsewhen(io.inputChannel.bits(i)(j).asSInt < min.asSInt(w.W)) {
        result(i)(j) := valAtMin.U
      }.elsewhen(io.inputChannel.bits(i)(j) < halfWayNeg.U || io.inputChannel.bits(i)(j) > halfWayPos.U) {
        result(i)(j) := io.inputChannel.bits(i)(j)
      }.otherwise({
        val sign = io.inputChannel.bits(i)(j)(w - 1)
        result(i)(j) := sign ## io.inputChannel.bits(i)(j) >> 1
      })
       */

      when(io.inputChannel.bits(i)(j).asSInt > max.asSInt(w.W)) {
        result(i)(j) := valAtMax.U
      }.elsewhen(io.inputChannel.bits(i)(j).asSInt < min.asSInt(w.W)) {
        result(i)(j) := valAtMin.U
      }.otherwise({
        result(i)(j) := io.inputChannel.bits(i)(j)
      })
    }
  }

  private val interfaceFSM = Module(new NoCalculationDelayInterfaceFSM)
  interfaceFSM.io.inputValid := io.inputChannel.valid
  interfaceFSM.io.outputReady := io.outputChannel.ready

  io.outputChannel.valid := interfaceFSM.io.outputValid
  io.inputChannel.ready := interfaceFSM.io.inputReady

  val buffer = RegInit(VecInit.fill(shape._1, shape._2)(0.U(w.W)))
  when(interfaceFSM.io.storeResult) {
    buffer := result
  }
  io.outputChannel.bits := buffer
}
