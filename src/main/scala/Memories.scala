import chisel3._
import chisel3.util.log2Ceil
class Memories(w : Int = 8, dimension : Int = 4, initialDataMemoryState : Array[Int], initialWeightsMemoryState : Array[Int], initialBiasMemoryState : Array[Int], initialConfigMemoryState : Array[Int]) extends Module{
  val io = IO(new Bundle{
    val dataAddress = Input(UInt(log2Ceil(initialDataMemoryState.length).W))
    val weightsAddress = Input(UInt(log2Ceil(initialWeightsMemoryState.length).W))
    val biasAddress = Input(UInt(log2Ceil(initialBiasMemoryState.length).W))
    val configAddress = Input(UInt(log2Ceil(initialConfigMemoryState.length).W))

    val dataRead = Output(Vec(dimension, UInt(w.W)))
    val weightsRead = Output(Vec(dimension, UInt(w.W)))
    val biasRead = Output(Vec(dimension, UInt(w.W)))

    val configRead = Output(UInt((log2Ceil(w)+1).W))

    val write = Input(Bool())
    val dataWrite = Input(Vec(dimension, UInt(w.W)))
  })

  val dataMemory = RegInit(VecInit(initialDataMemoryState.toIndexedSeq.map(_.S(w.W).asUInt)))
  val weightsMemory = RegInit(VecInit(initialWeightsMemoryState.toIndexedSeq.map(_.S(w.W).asUInt)))
  val biasMemory = RegInit(VecInit(initialBiasMemoryState.toIndexedSeq.map(_.S(w.W).asUInt)))

  val configMemory = RegInit(VecInit(initialConfigMemoryState.toIndexedSeq.map(_.S((log2Ceil(w)+1).W).asUInt))) //fixed_point encoding + if signed

  for (i <- 0 until dimension) {
    io.dataRead(i) := dataMemory(io.dataAddress+i.U)
    io.weightsRead(i) := weightsMemory(io.weightsAddress+i.U)
    io.biasRead(i) := biasMemory(io.biasAddress+i.U)

    when(io.write) {
      dataMemory(io.dataAddress + i.U) := io.dataWrite(i)
    }
  }

  io.configRead := configMemory(io.configAddress)
}
