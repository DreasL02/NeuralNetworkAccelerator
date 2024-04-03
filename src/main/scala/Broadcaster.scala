import chisel3._
import chisel3.util.DecoupledIO

class Broadcaster(
                   val w: Int = 8,
                   val dimensionsInput: (Int, Int, Int, Int) = (4, 4, 4, 4),
                   val dimensionsOutput: (Int, Int, Int, Int) = (4, 4, 4, 4),
                 ) extends Module {
  // Implements numpy style broadcasting for 4D tensors
  // The input tensor is broadcasted to the output tensor

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(dimensionsInput._1, Vec(dimensionsInput._2, Vec(dimensionsInput._3, Vec(dimensionsInput._4, UInt(w.W)))))))
    val resultChannel = new DecoupledIO(Vec(dimensionsOutput._1, Vec(dimensionsOutput._2, Vec(dimensionsOutput._3, Vec(dimensionsOutput._4, UInt(w.W))))))
  })

  for (i <- 0 until dimensionsOutput._1) {
    for (j <- 0 until dimensionsOutput._2) {
      for (k <- 0 until dimensionsOutput._3) {
        for (l <- 0 until dimensionsOutput._4) {
          val iInput = i % dimensionsInput._1
          val jInput = j % dimensionsInput._2
          val kInput = k % dimensionsInput._3
          val lInput = l % dimensionsInput._4

          io.resultChannel.bits(i)(j)(k)(l) := io.inputChannel.bits(iInput)(jInput)(kInput)(lInput)
        }
      }
    }
  }

  io.resultChannel.valid := io.inputChannel.valid
  io.inputChannel.ready := io.resultChannel.ready && io.resultChannel.valid
}
