package stages

import chisel3._
import onnx.Operators.InitializerType
import operators.{Initializer4d}

class InitializerStage(
                        wOut: Int,
                        shapeOut: (Int, Int, Int, Int),
                        data: Array[Array[Array[Array[BigInt]]]]
                      ) extends Stage0(wOut, shapeOut) {
  def this(initializerType: InitializerType) = this(initializerType.w, initializerType.shape, initializerType.data)

  val initializer = Module(new Initializer4d(wOut, shapeOut, data))

  io.outputChannel <> initializer.io.outputChannel
}
