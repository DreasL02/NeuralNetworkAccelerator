import chisel3._

class Initializer(w: Int = 8,
                  numberOfRows: Int = 4,
                  numberOfColumns: Int = 4,
                  data: Seq[Seq[Int]],
                 ) extends Module {
  def this(initializerType: onnx.Operators.InitializerType) = this(
    initializerType.w,
    initializerType.dimensions._1,
    initializerType.dimensions._2,
    initializerType.data
  )

  val io = IO(new Bundle {
    val output = Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))
    val valid = Output(Bool()) // indicates that the module should be done
  })

  for (i <- 0 until numberOfRows) {
    for (j <- 0 until numberOfColumns) {
      io.output(i)(j) := data(i)(j).asUInt(w.W)
    }
  }
  io.valid := true.B
}
