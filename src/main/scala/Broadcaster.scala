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

  for (newDim1 <- 0 until newDimensions._1) {
    for (newDim2 <- 0 until newDimensions._2) {
      for (newDim3 <- 0 until newDimensions._3) {
        for (newDim4 <- 0 until newDimensions._4) {
          // indices for the input tensor can be calculated by taking the modulus of the new dimensions
          val oldDim1 = newDim1 % operandDimensions._1
          val oldDim2 = newDim2 % operandDimensions._2
          val oldDim3 = newDim3 % operandDimensions._3
          val oldDim4 = newDim4 % operandDimensions._4

          io.resultChannel.bits(newDim1)(newDim2)(newDim3)(newDim4) := io.inputChannel.bits(oldDim1)(oldDim2)(oldDim3)(oldDim4)
        }
      }
    }
  }

  io.resultChannel.valid := io.inputChannel.valid
  io.inputChannel.ready := io.resultChannel.ready
}
