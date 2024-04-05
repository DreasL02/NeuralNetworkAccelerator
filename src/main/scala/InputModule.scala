import chisel3._
import chisel3.util.DecoupledIO

class InputModule(
                   val w: Int,
                   val dimensions: (Int, Int, Int, Int),
                 ) extends Module {

  def this(inputType: onnx.Operators.InputType) = this(inputType.w, inputType.dimensions)

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(dimensions._1, Vec(dimensions._2, Vec(dimensions._3, Vec(dimensions._4, UInt(w.W)))))))
    val outputChannel = new DecoupledIO(Vec(dimensions._1, Vec(dimensions._2, Vec(dimensions._3, Vec(dimensions._4, UInt(w.W))))))
  })

  io.outputChannel <> io.inputChannel
}
