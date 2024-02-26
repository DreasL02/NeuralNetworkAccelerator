package memories

import chisel3._
import chisel3.util.log2Ceil

// https://www.chisel-lang.org/docs/explanations/memories#single-ported

class SyncRAM(w: Int = 8, size: Int = 32) extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(log2Ceil(size).W))

    val readEnable = Input(Bool())
    val dataRead = Output(UInt(w.W))

    val writeEnable = Input(Bool())
    val dataWrite = Input(UInt(w.W))
  })

  // Initialize memories using the vectorized initial states
  val memory = SyncReadMem(size, UInt(w.W))

  io.dataRead := 0.U

  when(io.readEnable) {
    io.dataRead := memory(io.address)
  }

  when(io.writeEnable) {
    memory(io.address) := io.dataWrite

    when(io.readEnable) {
      io.dataRead := io.dataWrite // immediate bypass
    }
  }
}
