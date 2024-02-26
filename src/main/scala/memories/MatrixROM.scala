package memories

import chisel3._
import chisel3.util.log2Ceil

class MatrixROM(w: Int = 8, numberOfEntries: Int = 16, initialMatrixMemoryState: Array[Array[BigInt]]) extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(log2Ceil(initialMatrixMemoryState(0).length).W))

    val readEnable = Input(Bool())
    val dataRead = Output(Vec(numberOfEntries, UInt(w.W)))
  })

  // Initialize memories using the vectorized initial states
  val matrixMemory = for (i <- 0 until numberOfEntries) yield {
    val memory = Module(new ReadOnlyMemory(w, initialMatrixMemoryState(i))) // create a memory module
    memory // return module
  }

  // Matrix memories
  for (i <- 0 until numberOfEntries) {
    matrixMemory(i).io.address := io.address
    matrixMemory(i).io.readEnable := io.readEnable

    io.dataRead(i) := matrixMemory(i).io.dataRead
  }
}
