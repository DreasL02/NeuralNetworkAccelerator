package memories

import chisel3._
import chisel3.util.log2Ceil

// https://www.chisel-lang.org/docs/explanations/memories#single-ported

class ReadOnlyMemory(w: Int = 8, initialMemoryState: Array[Int]) extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(log2Ceil(initialMemoryState.length).W))

    val readEnable = Input(Bool())
    val dataRead = Output(UInt(w.W))
  })

  // TODO: Use SyncReadMem instead of RegInit
  // Look at different ways to initialize the memory
  // We may have to use a external file to initialize the memory
  // In which case we would have to refactor the code
  // val memory = SyncReadMem(initialMemoryState.length, UInt(w.W))

  // Initialize memories using the vectorized initial states
  val memory = VecInit(initialMemoryState.toIndexedSeq.map(_.S(w.W).asUInt))

  io.dataRead := 0.U
  when(io.readEnable) {
    io.dataRead := memory(io.address)
  }
}
