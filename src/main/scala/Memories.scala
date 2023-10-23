import chisel3._
import chisel3.util.log2Ceil

class Memories(w: Int = 8, dimension: Int = 4, initialInputsMemoryState: Array[Int], initialWeightsMemoryState: Array[Int],
               initialBiasMemoryState: Array[Int], initialSignsMemoryState: Array[Int], initialFixedPointMemoryState: Array[Int]) extends Module {
  val io = IO(new Bundle {
    val dataAddress = Input(UInt(log2Ceil(initialInputsMemoryState.length).W))

    val configAddress = Input(UInt(log2Ceil(initialFixedPointMemoryState.length).W))

    val read = Input(Bool())
    val dataRead = Output(Vec(dimension * dimension, UInt(w.W)))
    val weightsRead = Output(Vec(dimension * dimension, UInt(w.W)))
    val biasRead = Output(Vec(dimension * dimension, UInt(w.W)))

    val signsRead = Output(UInt((1.W)))
    val fixedPointRead = Output(UInt(log2Ceil(w).W))

    val write = Input(Bool())
    val writeAddress = Input(UInt(log2Ceil(initialInputsMemoryState.length).W))
    val dataWrite = Input(Vec(dimension * dimension, UInt(w.W)))
  })

  val inputsMemory = RegInit(VecInit(initialInputsMemoryState.toIndexedSeq.map(_.S(w.W).asUInt)))
  val weightsMemory = RegInit(VecInit(initialWeightsMemoryState.toIndexedSeq.map(_.S(w.W).asUInt)))
  val biasMemory = RegInit(VecInit(initialBiasMemoryState.toIndexedSeq.map(_.S(w.W).asUInt)))
  val signsMemory = RegInit(VecInit(initialSignsMemoryState.toIndexedSeq.map(_.S(1.W).asUInt)))
  val fixedPointsMemory = RegInit(VecInit(initialFixedPointMemoryState.toIndexedSeq.map(_.S(log2Ceil(w).W).asUInt)))

  for (i <- 0 until dimension * dimension) {
    io.dataRead(i) := 0.U
    io.weightsRead(i) := 0.U
    io.biasRead(i) := 0.U

    when(io.read) {
      io.dataRead(i) := inputsMemory(io.dataAddress + i.U)
      io.weightsRead(i) := weightsMemory(io.dataAddress + i.U)
      io.biasRead(i) := biasMemory(io.dataAddress + i.U)
    }

    when(io.write) {
      inputsMemory(io.writeAddress + i.U) := io.dataWrite(i)
    }
  }

  io.signsRead := 0.U
  io.fixedPointRead := 0.U
  when(io.read) {
    io.signsRead := signsMemory(io.configAddress)
    io.fixedPointRead := fixedPointsMemory(io.configAddress)
  }
}
