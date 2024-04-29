package operators

import chisel3._
import chisel3.util.DecoupledIO

class Tanh4d(
              w: Int,
              shape: (Int, Int, Int, Int),
              fixedPoint: Int,
              signed: Boolean,
            ) extends Module {

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(shape._1, Vec(shape._2, Vec(shape._3, Vec(shape._4, UInt(w.W)))))))
    val outputChannel = new DecoupledIO(Vec(shape._1, Vec(shape._2, Vec(shape._3, Vec(shape._4, UInt(w.W))))))
  })

  assert(signed == true, "Tanh only supports signed values")

  private val tanhs = VecInit.fill(shape._1, shape._2)(Module(new Tanh(w, (shape._3, shape._4), fixedPoint, signed)).io)

  for (i <- 0 until shape._1) {
    for (j <- 0 until shape._2) {
      tanhs(i)(j).inputChannel.valid := io.inputChannel.valid
      tanhs(i)(j).inputChannel.bits := io.inputChannel.bits(i)(j)

      io.outputChannel.bits(i)(j) := tanhs(i)(j).outputChannel.bits
      tanhs(i)(j).outputChannel.ready := io.outputChannel.ready
    }
  }

  // if one is _, then  all are _.
  io.inputChannel.ready := tanhs(0)(0).inputChannel.ready
  io.outputChannel.valid := tanhs(0)(0).outputChannel.valid
}
