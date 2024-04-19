import chisel3._
import chisel3.util.DecoupledIO
import module_utils.InterfaceFSM
import module_utils.SmallModules.timer
import scala_utils.Optional.optional

// ONNX Add operator in module form
class Add(w: Int = 8, numberOfRows: Int = 4, numberOfColumns: Int = 4, enableDebuggingIO: Boolean = true) extends Module {
  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W)))))
    val biasChannel = Flipped(new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W)))))

    val resultChannel = new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))

    val debugBiases = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W)))))
  })

  val result = Wire(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))
  // adds the values and biases together
  for (row <- 0 until numberOfRows) {
    for (column <- 0 until numberOfColumns) {
      result(row)(column) := io.inputChannel.bits(row)(column) + io.biasChannel.bits(row)(column)
    }
  }

  if (enableDebuggingIO) {
    io.debugBiases.get := io.biasChannel.bits
  }

  private val cyclesUntilOutputValid: Int = 0
  private val interfaceFSM = Module(new InterfaceFSM)
  interfaceFSM.io.inputValid := io.inputChannel.valid && io.biasChannel.valid
  interfaceFSM.io.outputReady := io.resultChannel.ready
  interfaceFSM.io.doneWithCalculation := timer(cyclesUntilOutputValid, interfaceFSM.io.calculateStart)

  io.resultChannel.valid := interfaceFSM.io.outputValid
  io.inputChannel.ready := interfaceFSM.io.inputReady
  io.biasChannel.ready := interfaceFSM.io.inputReady

  val buffer = RegInit(VecInit.fill(numberOfRows, numberOfColumns)(0.U(w.W)))
  when(interfaceFSM.io.storeResult) {
    buffer := result
  }
  io.resultChannel.bits := buffer
}
