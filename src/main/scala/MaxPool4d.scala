import chisel3._
import chisel3.util.DecoupledIO

class MaxPool4d(
                 w: Int = 8,
                 inputDimensions: (Int, Int, Int, Int) = (32, 32, 32, 32), // batch size, number of input channels, height, width
                 kernelShape: (Int, Int) = (2, 2),
                 pads: (Int, Int) = (0, 0),
                 strides: (Int, Int) = (2, 2),
               ) extends Module {

  def this(maxPoolType: onnx.Operators.MaxPoolType) = this(maxPoolType.w, maxPoolType.inputDimensions, maxPoolType.kernelShape, maxPoolType.pads, maxPoolType.strides)

  val outputShape = (
    inputDimensions._1,
    inputDimensions._2,
    (inputDimensions._3 - kernelShape._1 + 2 * pads._1) / strides._1 + 1,
    (inputDimensions._4 - kernelShape._2 + 2 * pads._2) / strides._2 + 1
  )

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(inputDimensions._1, Vec(inputDimensions._2, Vec(inputDimensions._3, Vec(inputDimensions._4, UInt(w.W)))))))
    val resultChannel = new DecoupledIO(Vec(outputShape._1, Vec(outputShape._2, Vec(outputShape._3, Vec(outputShape._4, UInt(w.W))))))
  })

  private val maxPools = VecInit.fill(inputDimensions._1, inputDimensions._2)(Module(new MaxPool(w, inputDimensions._3, inputDimensions._4, kernelShape, pads, strides)).io)

  for (i <- 0 until inputDimensions._1) {
    for (j <- 0 until inputDimensions._2) {
      maxPools(i)(j).inputChannel.valid := io.inputChannel.valid
      maxPools(i)(j).inputChannel.bits := io.inputChannel.bits(i)(j)

      maxPools(i)(j).resultChannel.ready := io.resultChannel.ready
      io.resultChannel.bits(i)(j) := maxPools(i)(j).resultChannel.bits
    }
  }
  
  io.inputChannel.ready := maxPools(0)(0).inputChannel.ready
  io.resultChannel.valid := maxPools(0)(0).resultChannel.valid
}
