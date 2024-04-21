package operators

import chisel3._
import chisel3.util.{DecoupledIO}

class ReLU4d(
              w: Int,
              shape: (Int, Int, Int, Int),
              signed: Boolean
            ) extends Module {

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(shape._1, Vec(shape._2, Vec(shape._3, Vec(shape._4, UInt(w.W)))))))
    val outputChannel = new DecoupledIO(Vec(shape._1, Vec(shape._2, Vec(shape._3, Vec(shape._4, UInt(w.W))))))
  })

  private val relus = VecInit.fill(shape._1, shape._2)(Module(new ReLU(w, shape._3, shape._4, signed)).io)

  for (i <- 0 until shape._1) {
    for (j <- 0 until shape._2) {

      relus(i)(j).inputChannel.valid := io.inputChannel.valid
      relus(i)(j).inputChannel.bits := io.inputChannel.bits(i)(j)

      io.outputChannel.bits(i)(j) := relus(i)(j).outputChannel.bits
      relus(i)(j).outputChannel.ready := io.outputChannel.ready
    }
  }

  // if one is _, then  all are _.
  io.inputChannel.ready := relus(0)(0).inputChannel.ready
  io.outputChannel.valid := relus(0)(0).outputChannel.valid

}
