package operators

import chisel3._
import chisel3.util.DecoupledIO

class Reshape(
               w: Int,
               inputShape: (Int, Int, Int, Int),
               shapeShape: (Int, Int, Int, Int),
               newShape: (Int, Int, Int, Int),
             ) extends Module {
  assert(inputShape._1 * inputShape._2 * inputShape._3 * inputShape._4 == newShape._1 * newShape._2 * newShape._3 * newShape._4, "Total sum of dimensions must be the same")

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(inputShape._1, Vec(inputShape._2, Vec(inputShape._3, Vec(inputShape._4, UInt(w.W)))))))
    val shapeChannel = Flipped(new DecoupledIO(Vec(shapeShape._1, Vec(shapeShape._2, Vec(shapeShape._3, Vec(shapeShape._4, UInt(1.W)))))))
    val outputChannel = new DecoupledIO(Vec(newShape._1, Vec(newShape._2, Vec(newShape._3, Vec(newShape._4, UInt(w.W))))))
  })

  private val flatInput = Wire(Vec(inputShape._1 * inputShape._2 * inputShape._3 * inputShape._4, UInt(w.W)))

  for (i <- 0 until inputShape._1) {
    for (j <- 0 until inputShape._2) {
      for (k <- 0 until inputShape._3) {
        for (l <- 0 until inputShape._4) {
          flatInput(i * inputShape._2 * inputShape._3 * inputShape._4 + j * inputShape._3 * inputShape._4 + k * inputShape._4 + l) := io.inputChannel.bits(i)(j)(k)(l)
        }
      }
    }
  }

  for (i <- 0 until newShape._1) {
    for (j <- 0 until newShape._2) {
      for (k <- 0 until newShape._3) {
        for (l <- 0 until newShape._4) {
          io.outputChannel.bits(i)(j)(k)(l) := flatInput(i * newShape._2 * newShape._3 * newShape._4 + j * newShape._3 * newShape._4 + k * newShape._4 + l)
        }
      }
    }
  }


  io.outputChannel.valid := io.inputChannel.valid
  io.inputChannel.ready := io.outputChannel.ready

  io.shapeChannel.ready := true.B
}
