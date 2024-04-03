import chisel3._
import chisel3.util.DecoupledIO

class Reshape(
               w: Int = 8,
               inputShape: (Int, Int, Int, Int) = (32, 32, 32, 32),
               outputShape: (Int, Int, Int, Int) = (16, 64, 16, 32)
             ) extends Module {

  assert(inputShape._1 * inputShape._2 * inputShape._3 * inputShape._4 == outputShape._1 * outputShape._2 * outputShape._3 * outputShape._4)

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(inputShape._1, Vec(inputShape._2, Vec(inputShape._3, Vec(inputShape._4, UInt(w.W)))))))
    val resultChannel = new DecoupledIO(Vec(outputShape._1, Vec(outputShape._2, Vec(outputShape._3, Vec(outputShape._4, UInt(w.W))))))
  })

  val flatInput = Wire(Vec(inputShape._1 * inputShape._2 * inputShape._3 * inputShape._4, UInt(w.W)))

  for (i <- 0 until inputShape._1) {
    for (j <- 0 until inputShape._2) {
      for (k <- 0 until inputShape._3) {
        for (l <- 0 until inputShape._4) {
          flatInput(i * inputShape._2 * inputShape._3 * inputShape._4 + j * inputShape._3 * inputShape._4 + k * inputShape._4 + l) := io.inputChannel.bits(i)(j)(k)(l)
        }
      }
    }
  }

  for (i <- 0 until outputShape._1) {
    for (j <- 0 until outputShape._2) {
      for (k <- 0 until outputShape._3) {
        for (l <- 0 until outputShape._4) {
          io.resultChannel.bits(i)(j)(k)(l) := flatInput(i * outputShape._2 * outputShape._3 * outputShape._4 + j * outputShape._3 * outputShape._4 + k * outputShape._4 + l)
        }
      }
    }
  }


  io.resultChannel.valid := io.inputChannel.valid
  io.inputChannel.ready := io.resultChannel.ready && io.resultChannel.valid
}
