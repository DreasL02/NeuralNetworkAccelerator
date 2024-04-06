import chisel3._
import chisel3.util.DecoupledIO

class Initializer(w: Int = 8,
                  numberOfRows: Int = 4,
                  numberOfColumns: Int = 4,
                  data: Array[Array[BigInt]],
                 ) extends Module {

  val io = IO(new Bundle {
    val outputChannel = new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))
  })

  private val storage: Vec[Vec[UInt]] = VecInit.fill(numberOfRows, numberOfColumns)(0.U(w.W))
  for (i <- 0 until numberOfRows) {
    for (j <- 0 until numberOfColumns) {
      storage(i)(j) := data(i)(j).U(w.W)
    }
  }

  io.outputChannel.bits := storage
  io.outputChannel.valid := true.B
}
