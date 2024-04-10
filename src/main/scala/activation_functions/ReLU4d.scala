package activation_functions

import chisel3._
import chisel3.util.{DecoupledIO}

class ReLU4d(
              w: Int = 8,
              dimensions: (Int, Int, Int, Int) = (4, 4, 4, 4),
              signed: Boolean = true
            ) extends Module {

  def this(reluType: onnx.Operators.ReluType) = this(reluType.w, reluType.dimensions, reluType.signed)

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(dimensions._1, Vec(dimensions._2, Vec(dimensions._3, Vec(dimensions._4, UInt(w.W)))))))
    val resultChannel = new DecoupledIO(Vec(dimensions._1, Vec(dimensions._2, Vec(dimensions._3, Vec(dimensions._4, UInt(w.W)))))
    )
  })

  for (i <- 0 until dimensions._1) {
    for (j <- 0 until dimensions._2) {
      for (k <- 0 until dimensions._3) {
        for (l <- 0 until dimensions._4) {
          io.resultChannel.bits(i)(j)(k)(l) := io.inputChannel.bits(i)(j)(k)(l) //default is the same value

          if (signed) { //if the values are signed
            when(io.inputChannel.bits(i)(j)(k)(l) >> (w - 1).U === 1.U) { //if signed bit (@msb) is 1, the result is negative
              io.resultChannel.bits(i)(j)(k)(l) := 0.U //ReLU gives 0
            }
          }
        }
      }
    }
  }

  io.resultChannel.valid := io.inputChannel.valid // Output is valid as soon as input is valid
  io.inputChannel.ready := io.resultChannel.ready && io.resultChannel.valid // Ready to receive new inputs when the result channel is ready and valid
}
