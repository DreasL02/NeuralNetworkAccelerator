import chisel3._
import chisel3.util.DecoupledIO

class Initializer(w: Int = 8,
                  numberOfRows: Int = 4,
                  numberOfColumns: Int = 4,
                  data: Array[Array[BigInt]],
                 ) extends Module {

  def this(initializerType: onnx.Operators.InitializerType) = this(
    initializerType.w,
    initializerType.dimensions._1,
    initializerType.dimensions._2,
    initializerType.data
  )

  val io = IO(new Bundle {
    val outputChannel = new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))
  })

  val storage: Vec[Vec[UInt]] = VecInit.fill(numberOfRows, numberOfColumns)(0.U(w.W))
  for (i <- 0 until numberOfRows) {
    for (j <- 0 until numberOfColumns) {
      storage(i)(j) := data(i)(j).U
    }
  }

  io.outputChannel.bits := storage
  io.outputChannel.valid := true.B
}
