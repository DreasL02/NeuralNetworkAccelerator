package module_utils

import chisel3._
import chisel3.util.{Cat, DecoupledIO}

// Scalable byte splitter of a w bit input into a Vec of 8 bit outputs
class ByteSplitter(w: Int = 8) extends Module {
  private val numberOfBytes = (w.toFloat / 8.0f).ceil.toInt // number of bytes needed to represent a w bit number

  val io = IO(new Bundle {
    val input = Input(UInt(w.W))
    val output = Output(Vec(numberOfBytes, UInt(8.W)))
  })
  // splits in order: (0) is lsb, (numberOfBytes - 1) is msb
  var bitsNotGoneThrough = w // number of bits that has not gone through the splitter
  for (i <- 0 until numberOfBytes) {
    val byte = WireInit(0.U(8.W))

    if (bitsNotGoneThrough < 8) { // if there are less than 8 bits left
      byte := io.input(i * 8 + (bitsNotGoneThrough - 1), i * 8) // take the remaining bits
    } else { // if there are 8 or more bits left
      byte := io.input(i * 8 + 7, i * 8) // take the next 8 bits
    }

    io.output(i) := byte // output the byte
    bitsNotGoneThrough = bitsNotGoneThrough - 8 // decrement the number of bits left
  }
}


// Scalable byte splitter of a Vec of w bit inputs into a Vec of 8 bit outputs
class VectorIntoByteSplitter(w: Int, dimension: Int) extends Module {
  private val numberOfBytes = (w.toFloat / 8.0f).ceil.toInt // number of bytes needed to represent a w bit number

  val io = IO(new Bundle {
    val input = Input(Vec(dimension * dimension, UInt(w.W)))
    val output = Output(Vec(dimension * dimension * numberOfBytes, UInt(8.W)))
  })

  for (i <- 0 until dimension * dimension) { // for each vector element
    val byteSplitter = Module(new ByteSplitter(w))
    byteSplitter.io.input := io.input(i) // split the vector element into bytes
    for (j <- 0 until numberOfBytes) { // for each byte
      io.output(i * numberOfBytes + j) := byteSplitter.io.output(j) // output the byte
    }
  }
}


// Scalable byte splitter of a Vec of w bit inputs into a Vec of 8 bit outputs
class FlatVectorIntoByteSplitter(w: Int, length: Int) extends Module {
  private val numberOfBytes = (w.toFloat / 8.0f).ceil.toInt // number of bytes needed to represent a w bit number

  val io = IO(new Bundle {
    val input = Input(Vec(length, UInt(w.W)))
    val output = Output(Vec(length * numberOfBytes, UInt(8.W)))
  })

  for (i <- 0 until length) { // for each vector element
    val byteSplitter = Module(new ByteSplitter(w))
    byteSplitter.io.input := io.input(i) // split the vector element into bytes
    for (j <- 0 until numberOfBytes) { // for each byte
      io.output(i * numberOfBytes + j) := byteSplitter.io.output(j) // output the byte
    }
  }
}


// Scalable byte collector of a Vec of 8 bit inputs into a w bit output
class ByteCollector(w: Int = 8) extends Module {
  private val numberOfBytes = (w.toFloat / 8.0f).ceil.toInt // number of bytes needed to represent a w bit number
  val io = IO(new Bundle {
    // in order: (0) is lsb, (numberOfBytes - 1) is msb
    val input = Input(Vec(numberOfBytes, UInt(8.W)))
    val output = Output(UInt(w.W))
  })

  io.output := Cat(io.input.reverse) // concatenates the bytes in order specified above
}

// Scalable byte collector of a Vec of 8 bit inputs into a Vec of w bit outputs
class ByteIntoVectorCollector(w: Int, dimension: Int) extends Module {
  private val numberOfBytes = (w.toFloat / 8.0f).ceil.toInt // number of bytes needed to represent a w bit number

  val io = IO(new Bundle {
    val input = Input(Vec(dimension * dimension * numberOfBytes, UInt(8.W)))
    val output = Output(Vec(dimension * dimension, UInt(w.W)))
  })

  for (i <- 0 until dimension * dimension) { // for each vector element
    val byteCollector = Module(new ByteCollector(w))
    for (j <- 0 until numberOfBytes) { // for each byte
      byteCollector.io.input(j) := io.input(i * numberOfBytes + j) // collect the bytes into a vector element
    }
    io.output(i) := byteCollector.io.output // output the collected vector element
  }
}

class ByteIntoFlatVectorCollector(inputByteCount: Int, outputUnit: Int) extends Module {
  private val outputLength = (inputByteCount * 8) / outputUnit

  val io = IO(new Bundle {
    val inputChannel = Flipped(DecoupledIO(Vec(inputByteCount, UInt(8.W))))
    val outputChannel = Flipped(DecoupledIO(Vec(outputLength, UInt(outputUnit.W))))
  })

  io.outputChannel.ready := io.inputChannel.ready
  io.outputChannel.valid := io.inputChannel.valid

  val inputBits = io.inputChannel.bits.reduce(_ ## _)

  io.outputChannel.bits(7) := inputBits( 8,  0)
  io.outputChannel.bits(6) := inputBits(17,  9)
  io.outputChannel.bits(5) := inputBits(26, 18)
  io.outputChannel.bits(4) := inputBits(35, 27)
  io.outputChannel.bits(3) := inputBits(44, 36)
  io.outputChannel.bits(2) := inputBits(53, 45)
  io.outputChannel.bits(1) := inputBits(62, 54)
  io.outputChannel.bits(0) := inputBits(71, 63)
}
