import chisel3._
import chisel3.util.DecoupledIO
import module_utils.Adders
import scala_utils.Optional.optional

// ONNX Add operator in module form
class Add(w: Int = 8, numberOfRows: Int = 4, numberOfColumns: Int = 4, enableDebuggingIO: Boolean = true) extends Module {
  def this(addType: onnx.Operators.AddType, enableDebuggingIO: Boolean) = this(
    addType.wOperands,
    addType.operandDimensions._1,
    addType.operandDimensions._2,
    enableDebuggingIO
  )

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W)))))
    val biasChannel = Flipped(new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W)))))

    val resultChannel = new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))

    val debugBiases = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W)))))
  })

  val adders = Module(new Adders(w, numberOfRows, numberOfColumns))
  adders.io.operandA := io.inputChannel.bits
  adders.io.operandB := io.biasChannel.bits
  io.resultChannel.bits := adders.io.result

  if (enableDebuggingIO) {
    io.debugBiases.get := io.biasChannel.bits
  }

  io.resultChannel.valid := io.inputChannel.valid && io.biasChannel.valid // Output is valid as soon as both inputs are valid
  io.inputChannel.ready := io.resultChannel.ready && io.resultChannel.valid // Ready to receive new inputs when the result channel is ready and valid
  io.biasChannel.ready := io.resultChannel.ready && io.resultChannel.valid // Ready to receive new biases when the result channel is ready and valid
}
