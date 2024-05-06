package stages

import chisel3._
import onnx.Operators.{TanhType}
import operators.{Tanh4d}

class TanhStage(
                 w: Int,
                 shape: (Int, Int, Int, Int),
                 fixedPoint: Int,
                 signed: Boolean,
               ) extends Stage1(w, shape, w) {

  def this(tanhType: TanhType) = this(tanhType.w, tanhType.shape, tanhType.fixedPoint, tanhType.signed)

  val tanh = Module(new Tanh4d(w, shape, fixedPoint, signed))

  tanh.io.inputChannel <> io.inputChannel
  io.outputChannel <> tanh.io.outputChannel


}
