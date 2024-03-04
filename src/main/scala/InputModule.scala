import chisel3._

class InputModule(
                   val width: Int,
                   val dimensions: (Int, Int),
                 ) extends Module {

  def this(inputType: onnx.Operators.InputType) = this(inputType.w, inputType.dimensions)

  val io = IO(new Bundle {
    val ready = Input(Bool()) // indicates that the producer has new data to be processed
    val inputs = Input(Vec(dimensions._1, Vec(dimensions._2, UInt(width.W))))
    val valid = Output(Bool()) // indicates that the module should be done
    val outputs = Output(Vec(dimensions._1, Vec(dimensions._2, UInt(width.W))))
  })

  io.outputs := io.inputs
  io.valid := io.ready
}
