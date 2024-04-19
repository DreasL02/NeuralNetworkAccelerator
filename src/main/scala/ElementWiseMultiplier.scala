import chisel3._
import chisel3.util.{DecoupledIO, Fill}
import module_utils.InterfaceFSM
import module_utils.SmallModules.{mult, timer}

// Hadamard product

class ElementWiseMultiplier(
                             w: Int = 8,
                             wResult: Int = 32,
                             numberOfRows: Int = 4,
                             numberOfColumns: Int = 4,
                             signed: Boolean = true
                           ) extends Module {
  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W)))))
    val weightChannel = Flipped(new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W)))))

    val resultChannel = new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W))))
  })

  val results = Wire(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W))))
  for (row <- 0 until numberOfRows) {
    for (column <- 0 until numberOfColumns) {
      results(row)(column) := mult(io.inputChannel.bits(row)(column), io.weightChannel.bits(row)(column), w, wResult, signed)
    }
  }

  private val cyclesUntilOutputValid: Int = 0
  private val interfaceFSM = Module(new InterfaceFSM)
  interfaceFSM.io.inputValid := io.inputChannel.valid && io.weightChannel.valid
  interfaceFSM.io.outputReady := io.resultChannel.ready
  interfaceFSM.io.doneWithCalculation := timer(cyclesUntilOutputValid, interfaceFSM.io.calculateStart)

  io.resultChannel.valid := interfaceFSM.io.outputValid
  io.inputChannel.ready := interfaceFSM.io.inputReady
  io.weightChannel.ready := interfaceFSM.io.inputReady

  val buffer = RegInit(VecInit.fill(numberOfRows, numberOfColumns)(0.U(w.W)))
  when(interfaceFSM.io.storeResult) {
    buffer := results
  }
  io.resultChannel.bits := buffer
}
