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

  println("Initializer")
  println("rows: " + numberOfRows)
  println("columns: " + numberOfColumns)


  val io = IO(new Bundle {
    val output = Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))
    val valid = Output(Bool()) // indicates that the module should be done
  })

  val storage: Vec[Vec[UInt]] = VecInit.fill(numberOfRows, numberOfColumns)(0.U(w.W))
  for (i <- 0 until numberOfRows) {
    for (j <- 0 until numberOfColumns) {
      storage(i)(j) := data(i)(j).U
    }
  }
  io.output := storage
  io.valid := true.B

  println("Initlizier output: " + io.output)
}
