package operators

import chisel3._
import chisel3.util.DecoupledIO

class MaxPool4d(
                 w: Int,
                 inputShape: (Int, Int, Int, Int), // batch size, number of input channels, height, width
                 kernelShape: (Int, Int),
                 pads: (Int, Int),
                 strides: (Int, Int),
                 signed: Boolean
               ) extends Module {

  val outputShape = (
    inputShape._1,
    inputShape._2,
    (inputShape._3 - kernelShape._1 + 2 * pads._1) / strides._1 + 1,
    (inputShape._4 - kernelShape._2 + 2 * pads._2) / strides._2 + 1
  )

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(inputShape._1, Vec(inputShape._2, Vec(inputShape._3, Vec(inputShape._4, UInt(w.W)))))))
    val outputChannel = new DecoupledIO(Vec(outputShape._1, Vec(outputShape._2, Vec(outputShape._3, Vec(outputShape._4, UInt(w.W))))))
  })

  private val maxPools = VecInit.fill(inputShape._1, inputShape._2)(Module(new MaxPool(w, inputShape._3, inputShape._4, kernelShape, pads, strides, signed)).io)

  for (i <- 0 until inputShape._1) {
    for (j <- 0 until inputShape._2) {
      maxPools(i)(j).inputChannel.valid := io.inputChannel.valid
      maxPools(i)(j).inputChannel.bits := io.inputChannel.bits(i)(j)

      maxPools(i)(j).outputChannel.ready := io.outputChannel.ready
      io.outputChannel.bits(i)(j) := maxPools(i)(j).outputChannel.bits
    }
  }

  io.inputChannel.ready := maxPools(0)(0).inputChannel.ready
  io.outputChannel.valid := maxPools(0)(0).outputChannel.valid
}
