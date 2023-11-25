import chisel3._
import chisel3.util.Cat

class ByteSplitter(w: Int = 8) extends Module {
  private val numberOfBytes = (w.toFloat / 8.0f).ceil.toInt

  val io = IO(new Bundle {
    val input = Input(UInt(w.W))
    val output = Output(Vec(numberOfBytes, UInt(8.W)))
  })
  // splits in order: (0) is lsb, (numberOfBytes - 1) is msb
  for (i <- 0 until numberOfBytes) {
    io.output(i) := io.input(i * 8 + 7, i * 8)
  }
}

class ByteCollector(w: Int = 8) extends Module {
  private val numberOfBytes = (w.toFloat / 8.0f).ceil.toInt
  val io = IO(new Bundle {
    val input = Input(Vec(numberOfBytes, UInt(8.W)))
    val output = Output(UInt(w.W))
  })

  io.output := Cat(io.input.reverse)
}

