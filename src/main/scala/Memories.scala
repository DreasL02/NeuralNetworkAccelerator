import chisel3._
import chisel3.util.log2Ceil

class Memories(w: Int = 8, dimension: Int = 4, initialInputsMemoryState: Array[Int], initialWeightsMemoryState: Array[Int],
               initialBiasMemoryState: Array[Int], initialSignsMemoryState: Array[Int], initialFixedPointMemoryState: Array[Int]) extends Module {
  val io = IO(new Bundle {
    val matrixAddress = Input(UInt(log2Ceil(initialInputsMemoryState.length).W))

    val vectorAddress = Input(UInt(log2Ceil(initialFixedPointMemoryState.length).W))

    val readEnable = Input(Bool())
    val inputsRead = Output(Vec(dimension * dimension, UInt(w.W)))
    val weightsRead = Output(Vec(dimension * dimension, UInt(w.W)))
    val biasRead = Output(Vec(dimension * dimension, UInt(w.W)))

    val signsRead = Output(UInt((1.W)))
    val fixedPointRead = Output(UInt(log2Ceil(w).W))

    val writeEnable = Input(Bool())
    val inputsWrite = Input(Vec(dimension * dimension, UInt(w.W)))
  })

  // Initialize memories using the vectorized initial states
  val inputsMemory = RegInit(VecInit(initialInputsMemoryState.toIndexedSeq.map(_.S(w.W).asUInt)))
  val weightsMemory = RegInit(VecInit(initialWeightsMemoryState.toIndexedSeq.map(_.S(w.W).asUInt)))
  val biasMemory = RegInit(VecInit(initialBiasMemoryState.toIndexedSeq.map(_.S(w.W).asUInt)))
  val signsMemory = RegInit(VecInit(initialSignsMemoryState.toIndexedSeq.map(_.S(1.W).asUInt)))
  val fixedPointsMemory = RegInit(VecInit(initialFixedPointMemoryState.toIndexedSeq.map(_.S(log2Ceil(w).W).asUInt)))

  // Matrix memories
  for (i <- 0 until dimension * dimension) {
    // Default read values
    io.inputsRead(i) := 0.U
    io.weightsRead(i) := 0.U
    io.biasRead(i) := 0.U

    when(io.readEnable) {
      // emit the values at the address up to address + dimension * dimension
      io.inputsRead(i) := inputsMemory(io.matrixAddress + i.U)
      io.weightsRead(i) := weightsMemory(io.matrixAddress + i.U)
      io.biasRead(i) := biasMemory(io.matrixAddress + i.U)
    }

    when(io.writeEnable) {
      // write the values at the address up to address + dimension * dimension
      inputsMemory(io.matrixAddress + i.U) := io.inputsWrite(i)
      io.inputsRead(i) := io.inputsWrite(i) // immediate bypass
    }
  }

  io.signsRead := 0.U // default read value
  io.fixedPointRead := 0.U // default read value
  when(io.readEnable) {
    io.signsRead := signsMemory(io.vectorAddress) // emit the value at the address
    io.fixedPointRead := fixedPointsMemory(io.vectorAddress)  // emit the value at the address
  }
}
