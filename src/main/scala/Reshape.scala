import chisel3._
import chisel3.util.DecoupledIO

class Reshape(
               w: Int = 8,
               inputDimensions: (Int, Int, Int, Int) = (32, 32, 32, 32),
               shapeDimensions: (Int, Int, Int, Int) = (16, 64, 16, 32)
             ) extends Module {

  assert(inputDimensions._1 * inputDimensions._2 * inputDimensions._3 * inputDimensions._4 == shapeDimensions._1 * shapeDimensions._2 * shapeDimensions._3 * shapeDimensions._4)

  def this(reshapeType: onnx.Operators.ReshapeType) = this(reshapeType.w, reshapeType.inputDimensions, reshapeType.shapeDimensions)

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(inputDimensions._1, Vec(inputDimensions._2, Vec(inputDimensions._3, Vec(inputDimensions._4, UInt(w.W)))))))
    val shapeChannel = Flipped(new DecoupledIO(Vec(shapeDimensions._1, Vec(shapeDimensions._2, Vec(shapeDimensions._3, Vec(shapeDimensions._4, UInt(1.W)))))))
    val resultChannel = new DecoupledIO(Vec(shapeDimensions._1, Vec(shapeDimensions._2, Vec(shapeDimensions._3, Vec(shapeDimensions._4, UInt(w.W))))))
  })

  val flatInput = Wire(Vec(inputDimensions._1 * inputDimensions._2 * inputDimensions._3 * inputDimensions._4, UInt(w.W)))

  for (i <- 0 until inputDimensions._1) {
    for (j <- 0 until inputDimensions._2) {
      for (k <- 0 until inputDimensions._3) {
        for (l <- 0 until inputDimensions._4) {
          flatInput(i * inputDimensions._2 * inputDimensions._3 * inputDimensions._4 + j * inputDimensions._3 * inputDimensions._4 + k * inputDimensions._4 + l) := io.inputChannel.bits(i)(j)(k)(l)
        }
      }
    }
  }

  for (i <- 0 until shapeDimensions._1) {
    for (j <- 0 until shapeDimensions._2) {
      for (k <- 0 until shapeDimensions._3) {
        for (l <- 0 until shapeDimensions._4) {
          io.resultChannel.bits(i)(j)(k)(l) := flatInput(i * shapeDimensions._2 * shapeDimensions._3 * shapeDimensions._4 + j * shapeDimensions._3 * shapeDimensions._4 + k * shapeDimensions._4 + l)
        }
      }
    }
  }


  io.resultChannel.valid := io.inputChannel.valid
  io.inputChannel.ready := io.resultChannel.ready && io.resultChannel.valid

  io.shapeChannel.ready := true.B
}
