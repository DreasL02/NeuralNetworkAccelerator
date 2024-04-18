import chisel3._
import chisel3.util.DecoupledIO
import scala_utils.Optional.optional

class Add4d(
             w: Int = 8,
             dimensionsInput: (Int, Int, Int, Int) = (4, 4, 4, 4),
             enableDebuggingIO: Boolean = true
           ) extends Module {

  def this(addType: onnx.Operators.AddType, enableDebuggingIO: Boolean) = this(addType.w, addType.operandDimensions, enableDebuggingIO)

  val dimensionsOutput = (dimensionsInput._1, dimensionsInput._2, dimensionsInput._3, dimensionsInput._4)

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(dimensionsInput._1, Vec(dimensionsInput._2, Vec(dimensionsInput._3, Vec(dimensionsInput._4, UInt(w.W)))))))
    val biasChannel = Flipped(new DecoupledIO(Vec(dimensionsInput._1, Vec(dimensionsInput._2, Vec(dimensionsInput._3, Vec(dimensionsInput._4, UInt(w.W)))))))

    val resultChannel = new DecoupledIO(Vec(dimensionsOutput._1, Vec(dimensionsOutput._2, Vec(dimensionsOutput._3, Vec(dimensionsOutput._4, UInt(w.W))))))

    val debugBiases = optional(enableDebuggingIO, Output(Vec(dimensionsInput._1, Vec(dimensionsInput._2, Vec(dimensionsInput._3, Vec(dimensionsInput._4, UInt(w.W)))))))
  })

  private val adds = VecInit.fill(dimensionsOutput._1, dimensionsOutput._2)(Module(new Add(w, dimensionsOutput._3, dimensionsOutput._4, enableDebuggingIO)).io)

  for (i <- 0 until dimensionsOutput._1) {
    for (j <- 0 until dimensionsOutput._2) {

      adds(i)(j).inputChannel.valid := io.inputChannel.valid
      adds(i)(j).inputChannel.bits := io.inputChannel.bits(i)(j)


      adds(i)(j).biasChannel.valid := io.biasChannel.valid
      adds(i)(j).biasChannel.bits := io.biasChannel.bits(i)(j)

      io.resultChannel.bits(i)(j) := adds(i)(j).resultChannel.bits

      if (enableDebuggingIO) {
        io.debugBiases.get(i)(j) := adds(i)(j).debugBiases.get
      }

      adds(i)(j).resultChannel.ready := io.resultChannel.ready
    }
  }

  // if one is _, then  all are _.
  io.inputChannel.ready := adds(0)(0).inputChannel.ready
  io.biasChannel.ready := adds(0)(0).biasChannel.ready
  io.resultChannel.valid := adds(0)(0).resultChannel.valid
}
