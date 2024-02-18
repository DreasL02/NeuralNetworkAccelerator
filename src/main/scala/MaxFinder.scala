import chisel3._

class MaxFinder(w: Int = 8, xDimension: Int = 4, yDimension: Int = 4) extends Module {
  val io = IO(new Bundle {
    val inputs = Input(Vec(xDimension, Vec(yDimension, UInt(w.W))))
    val result = Output(UInt(w.W))
  })

  io.result := io.inputs.map(_.map(_.asUInt).reduce(_ max _)).reduce(_ max _)
}
