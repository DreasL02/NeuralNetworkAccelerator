import chisel3._
import chisel3.util.DecoupledIO

class Broadcaster(
                   val w: Int = 8,
                   val operandDimensions: (Int, Int, Int, Int) = (4, 4, 4, 4),
                   val newDimensions: (Int, Int, Int, Int) = (4, 4, 4, 4),
                 ) extends Module {
  // Implements numpy style broadcasting for 4D tensors
  // The input tensor is broadcasted to the output tensor

  def this(broadcasterType: onnx.Operators.BroadcasterType) = this(broadcasterType.w, broadcasterType.operandDimensions, broadcasterType.newDimensions)

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(operandDimensions._1, Vec(operandDimensions._2, Vec(operandDimensions._3, Vec(operandDimensions._4, UInt(w.W)))))))
    val resultChannel = new DecoupledIO(Vec(newDimensions._1, Vec(newDimensions._2, Vec(newDimensions._3, Vec(newDimensions._4, UInt(w.W))))))
  })

  for (i <- 0 until newDimensions._1) {
    for (j <- 0 until newDimensions._2) {
      for (k <- 0 until newDimensions._3) {
        for (l <- 0 until newDimensions._4) {
          val iInput = i % operandDimensions._1
          val jInput = j % operandDimensions._2
          val kInput = k % operandDimensions._3
          val lInput = l % operandDimensions._4

          io.resultChannel.bits(i)(j)(k)(l) := io.inputChannel.bits(iInput)(jInput)(kInput)(lInput)
        }
      }
    }
  }

  io.resultChannel.valid := io.inputChannel.valid
  io.inputChannel.ready := io.resultChannel.ready && io.resultChannel.valid
}
