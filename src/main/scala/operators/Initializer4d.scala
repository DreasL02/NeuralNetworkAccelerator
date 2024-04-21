package operators

import chisel3._
import chisel3.util.DecoupledIO

class Initializer4d(
                     val w: Int = 8,
                     val dimensions: (Int, Int, Int, Int) = (4, 4, 4, 4),
                     val data: Array[Array[Array[Array[BigInt]]]]
                   ) extends Module {
  val io = IO(new Bundle {
    val outputChannel = new DecoupledIO(Vec(dimensions._1, Vec(dimensions._2, Vec(dimensions._3, Vec(dimensions._4, UInt(w.W))))))
  })

  private val initializers = for (i <- 0 until dimensions._1) yield {
    for (j <- 0 until dimensions._2) yield {
      val initializer = Module(new Initializer(w, dimensions._3, dimensions._4, data(i)(j)))
      initializer.io.outputChannel.ready := io.outputChannel.ready
      initializer.io.outputChannel
    }
  }

  for (i <- 0 until dimensions._1) {
    for (j <- 0 until dimensions._2) {
      io.outputChannel.bits(i)(j) := initializers(i)(j).bits
    }
  }

  io.outputChannel.valid := initializers(0)(0).valid
}