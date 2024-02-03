package memories

import chisel3._
import chisel3.util.log2Ceil

class MatrixSyncRAM(w: Int = 8, xDimension: Int = 4, yDimension: Int = 4, initialMatrixMemoryState: Array[Array[Int]]) extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(log2Ceil(initialMatrixMemoryState(0).length).W))

    val readEnable = Input(Bool())
    val dataRead = Output(Vec(xDimension * yDimension, UInt(w.W)))

    val writeEnable = Input(Bool())
    val dataWrite = Input(Vec(xDimension * yDimension, UInt(w.W)))
  })

  // Initialize memories using the vectorized initial states
  val matrixMemory = for (i <- 0 until xDimension * yDimension) yield {
    val memory = Module(new SyncRAM(w, initialMatrixMemoryState(i))) // create a memory module
    memory // return module
  }

  // Matrix memories
  for (i <- 0 until xDimension * yDimension) {
    matrixMemory(i).io.address := io.address
    matrixMemory(i).io.readEnable := io.readEnable
    matrixMemory(i).io.writeEnable := io.writeEnable
    matrixMemory(i).io.dataWrite := io.dataWrite(i)

    io.dataRead(i) := matrixMemory(i).io.dataRead
  }
}