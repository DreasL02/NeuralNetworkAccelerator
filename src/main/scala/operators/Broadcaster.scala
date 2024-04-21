package operators

import chisel3._
import chisel3.util.DecoupledIO

class Broadcaster(
                   val w: Int,
                   val oldShape: (Int, Int, Int, Int),
                   val newShape: (Int, Int, Int, Int),
                 ) extends Module {

  // Implements numpy style broadcasting for 4D tensors
  // The input tensor is broadcasted to the output tensor

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(oldShape._1, Vec(oldShape._2, Vec(oldShape._3, Vec(oldShape._4, UInt(w.W)))))))
    val outputChannel = new DecoupledIO(Vec(newShape._1, Vec(newShape._2, Vec(newShape._3, Vec(newShape._4, UInt(w.W))))))
  })

  for (newDim1 <- 0 until newShape._1) {
    for (newDim2 <- 0 until newShape._2) {
      for (newDim3 <- 0 until newShape._3) {
        for (newDim4 <- 0 until newShape._4) {
          val oldDim1 = newDim1 % oldShape._1
          val oldDim2 = newDim2 % oldShape._2
          val oldDim3 = newDim3 % oldShape._3
          val oldDim4 = newDim4 % oldShape._4

          io.outputChannel.bits(newDim1)(newDim2)(newDim3)(newDim4) := io.inputChannel.bits(oldDim1)(oldDim2)(oldDim3)(oldDim4)
        }
      }
    }
  }

  io.outputChannel.valid := io.inputChannel.valid
  io.inputChannel.ready := io.outputChannel.ready
}
