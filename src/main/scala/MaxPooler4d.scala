import chisel3._
import chisel3.util.DecoupledIO

class MaxPooler4d(
                   w: Int = 8,
                   inputShape: (Int, Int, Int, Int) = (32, 32, 32, 32), // batch size, number of input channels, height, width
                   kernelShape: (Int, Int) = (2, 2),
                   pads: (Int, Int) = (0, 0),
                   strides: (Int, Int) = (2, 2),
                 ) extends Module {

  val outputShape = (
    inputShape._1,
    inputShape._2,
    (inputShape._3 - kernelShape._1 + 2 * pads._1) / strides._1 + 1,
    (inputShape._4 - kernelShape._2 + 2 * pads._2) / strides._2 + 1
  )

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(inputShape._1, Vec(inputShape._2, Vec(inputShape._3, Vec(inputShape._4, UInt(w.W)))))))
    val resultChannel = new DecoupledIO(Vec(outputShape._1, Vec(outputShape._2, Vec(outputShape._3, Vec(outputShape._4, UInt(w.W))))))
  })

  val maxPools = VecInit.fill(inputShape._1, inputShape._2)(Module(new MaxPool(w, inputShape._3, inputShape._4, kernelShape, pads, strides)).io)

  for (i <- 0 until inputShape._1) {
    for (j <- 0 until inputShape._2) {
      maxPools(i)(j).inputChannel.valid := io.inputChannel.valid
      maxPools(i)(j).inputChannel.bits := io.inputChannel.bits(i)(j)

      maxPools(i)(j).resultChannel.ready := io.resultChannel.ready
      io.resultChannel.bits(i)(j) := maxPools(i)(j).resultChannel.bits
    }
  }

  io.resultChannel.valid := maxPools.map(_.map(_.resultChannel.valid).reduce(_ && _)).reduce(_ && _)
  io.inputChannel.ready := maxPools.map(_.map(_.inputChannel.ready).reduce(_ && _)).reduce(_ && _) && io.resultChannel.valid && io.resultChannel.ready
}
