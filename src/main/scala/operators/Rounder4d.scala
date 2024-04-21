package operators

import chisel3._
import chisel3.util.DecoupledIO

class Rounder4d(
                 wBefore: Int,
                 wAfter: Int,
                 shape: (Int, Int, Int, Int),
                 signed: Boolean,
                 fixedPoint: Int
               ) extends Module {

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(shape._1, Vec(shape._2, Vec(shape._3, Vec(shape._4, UInt(wBefore.W)))))))
    val outputChannel = new DecoupledIO(Vec(shape._1, Vec(shape._2, Vec(shape._3, Vec(shape._4, UInt(wAfter.W)))))
    )
  })

  private val rounders = VecInit.fill(shape._1, shape._2)(Module(new Rounder(wBefore, wAfter, shape._3, shape._4, signed, fixedPoint)).io)

  for (i <- 0 until shape._1) {
    for (j <- 0 until shape._2) {
      rounders(i)(j).inputChannel.valid := io.inputChannel.valid
      rounders(i)(j).inputChannel.bits := io.inputChannel.bits(i)(j)

      rounders(i)(j).outputChannel.ready := io.outputChannel.ready
      io.outputChannel.bits(i)(j) := rounders(i)(j).outputChannel.bits
    }
  }

  io.inputChannel.ready := rounders(0)(0).inputChannel.ready
  io.outputChannel.valid := rounders(0)(0).outputChannel.valid
}
