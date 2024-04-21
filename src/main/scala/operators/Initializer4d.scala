package operators

import chisel3._
import chisel3.util.DecoupledIO

class Initializer4d(
                     val w: Int,
                     val shape: (Int, Int, Int, Int),
                     val data: Array[Array[Array[Array[BigInt]]]]
                   ) extends Module {
  val io = IO(new Bundle {
    val outputChannel = new DecoupledIO(Vec(shape._1, Vec(shape._2, Vec(shape._3, Vec(shape._4, UInt(w.W))))))
  })

  private val initializers = for (i <- 0 until shape._1) yield {
    for (j <- 0 until shape._2) yield {
      val initializer = Module(new Initializer(w, shape._3, shape._4, data(i)(j)))
      initializer.io.outputChannel.ready := io.outputChannel.ready
      initializer.io.outputChannel
    }
  }

  for (i <- 0 until shape._1) {
    for (j <- 0 until shape._2) {
      io.outputChannel.bits(i)(j) := initializers(i)(j).bits
    }
  }

  io.outputChannel.valid := initializers(0)(0).valid
}
