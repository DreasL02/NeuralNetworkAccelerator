package operators

import chisel3._
import chisel3.util.DecoupledIO

class Reshape(
               w: Int = 8,
               inputDimensions: (Int, Int, Int, Int) = (32, 32, 32, 32),
               shapeDimensions: (Int, Int, Int, Int) = (16, 64, 16, 32),
               newDimensions: (Int, Int, Int, Int) = (16, 64, 16, 32),
             ) extends Module {
  assert(inputDimensions._1 * inputDimensions._2 * inputDimensions._3 * inputDimensions._4 == newDimensions._1 * newDimensions._2 * newDimensions._3 * newDimensions._4, "Total sum of dimensions must be the same")

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(inputDimensions._1, Vec(inputDimensions._2, Vec(inputDimensions._3, Vec(inputDimensions._4, UInt(w.W)))))))
    val shapeChannel = Flipped(new DecoupledIO(Vec(shapeDimensions._1, Vec(shapeDimensions._2, Vec(shapeDimensions._3, Vec(shapeDimensions._4, UInt(1.W)))))))
    val outputChannel = new DecoupledIO(Vec(newDimensions._1, Vec(newDimensions._2, Vec(newDimensions._3, Vec(newDimensions._4, UInt(w.W))))))
  })

  private val flatInput = Wire(Vec(inputDimensions._1 * inputDimensions._2 * inputDimensions._3 * inputDimensions._4, UInt(w.W)))

  for (i <- 0 until inputDimensions._1) {
    for (j <- 0 until inputDimensions._2) {
      for (k <- 0 until inputDimensions._3) {
        for (l <- 0 until inputDimensions._4) {
          flatInput(i * inputDimensions._2 * inputDimensions._3 * inputDimensions._4 + j * inputDimensions._3 * inputDimensions._4 + k * inputDimensions._4 + l) := io.inputChannel.bits(i)(j)(k)(l)
        }
      }
    }
  }

  for (i <- 0 until newDimensions._1) {
    for (j <- 0 until newDimensions._2) {
      for (k <- 0 until newDimensions._3) {
        for (l <- 0 until newDimensions._4) {
          io.outputChannel.bits(i)(j)(k)(l) := flatInput(i * newDimensions._2 * newDimensions._3 * newDimensions._4 + j * newDimensions._3 * newDimensions._4 + k * newDimensions._4 + l)
        }
      }
    }
  }


  io.outputChannel.valid := io.inputChannel.valid
  io.inputChannel.ready := io.outputChannel.ready

  io.shapeChannel.ready := true.B
}
