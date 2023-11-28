import chisel3._
import chisel3.util.Cat

class ByteSplitter(w: Int = 8) extends Module {
  private val numberOfBytes = (w.toFloat / 8.0f).ceil.toInt

  val io = IO(new Bundle {
    val input = Input(UInt(w.W))
    val output = Output(Vec(numberOfBytes, UInt(8.W)))
  })
  // splits in order: (0) is lsb, (numberOfBytes - 1) is msb
  var bitsNotGoneThrough = w
  for (i <- 0 until numberOfBytes) {
    val byte = WireInit(0.U(8.W))
    if (bitsNotGoneThrough < 8) {
      byte := io.input(i * 8 + (bitsNotGoneThrough - 1), i * 8)
    } else {
      byte := io.input(i * 8 + 7, i * 8)
    }
    io.output(i) := byte
    bitsNotGoneThrough = bitsNotGoneThrough - 8
  }
}

class VectorIntoByteSplitter(w: Int, dimension: Int) extends Module {
  private val numberOfBytes = (w.toFloat / 8.0f).ceil.toInt

  val io = IO(new Bundle {
    val input = Input(Vec(dimension * dimension, UInt(w.W)))
    val output = Output(Vec(dimension * dimension * numberOfBytes, UInt(8.W)))
  })

  for (i <- 0 until dimension * dimension) {
    val byteSplitter = Module(new ByteSplitter(w))
    byteSplitter.io.input := io.input(i)
    for (j <- 0 until numberOfBytes) {
      io.output(i * numberOfBytes + j) := byteSplitter.io.output(j)
    }
  }
}

class ByteCollector(w: Int = 8) extends Module {
  private val numberOfBytes = (w.toFloat / 8.0f).ceil.toInt
  val io = IO(new Bundle {
    // in order: (0) is lsb, (numberOfBytes - 1) is msb
    val input = Input(Vec(numberOfBytes, UInt(8.W)))
    val output = Output(UInt(w.W))
  })

  io.output := Cat(io.input.reverse)
}

class ByteIntoVectorCollector(w: Int, dimension: Int) extends Module {
  private val numberOfBytes = (w.toFloat / 8.0f).ceil.toInt

  val io = IO(new Bundle {
    val input = Input(Vec(dimension * dimension * numberOfBytes, UInt(8.W)))
    val output = Output(Vec(dimension * dimension, UInt(w.W)))
  })

  for (i <- 0 until dimension * dimension) {
    val byteCollector = Module(new ByteCollector(w))
    for (j <- 0 until numberOfBytes) {
      byteCollector.io.input(j) := io.input(i * numberOfBytes + j)
    }
    io.output(i) := byteCollector.io.output
  }
}
