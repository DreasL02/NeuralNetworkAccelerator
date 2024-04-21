package stages

import chisel3._
import onnx.Operators.InitializerType
import operators.Initializer

class InitializerStage(
                        wOut: Int,
                        shapeOut: (Int, Int, Int, Int),
                        data: Array[Array[Array[Array[BigInt]]]]
                      ) extends Stage0(wOut, shapeOut) {
  def this(initializerType: InitializerType) = this(initializerType.w, initializerType.dimensions, initializerType.data)

  private val initializers = for (i <- 0 until shapeOut._1) yield {
    for (j <- 0 until shapeOut._2) yield {
      val initializer = Module(new Initializer(wOut, shapeOut._3, shapeOut._4, data(i)(j)))
      initializer.io.outputChannel.ready := io.outputChannel.ready
      initializer.io.outputChannel
    }
  }

  for (i <- 0 until shapeOut._1) {
    for (j <- 0 until shapeOut._2) {
      io.outputChannel.bits(i)(j) := initializers(i)(j).bits
    }
  }

  io.outputChannel.valid := initializers(0)(0).valid

  latency = 0
  dspUsage = 0
}
