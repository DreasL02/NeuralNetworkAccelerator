import chisel3._
import chisel3.util.DecoupledIO

class OutputModule(
                    val width: Int,
                    val dimensions: (Int, Int),
                  ) extends Module {

  def this(outputType: onnx.Operators.OutputType) = this(outputType.w, outputType.dimensions)

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(dimensions._1, Vec(dimensions._2, UInt(width.W)))))
    val outputChannel = new DecoupledIO(Vec(dimensions._1, Vec(dimensions._2, UInt(width.W))))
  })

  io.outputChannel <> io.inputChannel
}
