import chisel3._
import chisel3.util.{DecoupledIO, Fill}
import module_utils.SmallModules.mult


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

  for (row <- 0 until numberOfRows) {
    for (column <- 0 until numberOfColumns) {
      io.resultChannel.bits(row)(column) := mult(io.inputChannel.bits(row)(column), io.weightChannel.bits(row)(column), w, wResult, signed)
    }
  }

  io.resultChannel.valid := io.inputChannel.valid && io.weightChannel.valid
  io.inputChannel.ready := io.resultChannel.ready
  io.weightChannel.ready := io.resultChannel.ready
}
