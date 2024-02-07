import chisel3._
import chisel3.util.log2Ceil
import memories._

// Memory module that contains all the memories used for the accelerator
class Memories(w: Int = 8,
               wStore: Int = 32,
               xDimension: Int = 4,
               yDimension: Int = 4,
               initialInputsMemoryState: Array[Array[BigInt]],
               initialWeightsMemoryState: Array[Array[BigInt]],
               initialBiasMemoryState: Array[Array[BigInt]],
               initialSignsMemoryState: Array[BigInt],
               initialFixedPointMemoryState: Array[BigInt]
              ) extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(log2Ceil(initialFixedPointMemoryState.length).W))

    val readEnable = Input(Bool())
    val inputsRead = Output(Vec(xDimension * yDimension, UInt(w.W)))
    val weightsRead = Output(Vec(xDimension * yDimension, UInt(w.W)))
    val biasRead = Output(Vec(xDimension * yDimension, UInt(wStore.W)))

    val signsRead = Output(UInt(1.W))
    val fixedPointRead = Output(UInt(log2Ceil(w).W))

    val writeEnable = Input(Bool())
    val inputsWrite = Input(Vec(xDimension * yDimension, UInt(w.W)))
  })

  // Initialize memories using the vectorized initial states
  val inputsMemory = Module(new MatrixSyncRAM(w, xDimension, yDimension, initialInputsMemoryState))
  val weightsMemory = Module(new MatrixROM(w, xDimension, yDimension, initialWeightsMemoryState))
  val biasMemory = Module(new MatrixROM(wStore, xDimension, yDimension, initialBiasMemoryState))
  val signsMemory = Module(new ReadOnlyMemory(w, initialSignsMemoryState))
  val fixedPointsMemory = Module(new ReadOnlyMemory(log2Ceil(w), initialFixedPointMemoryState))


  // Matrix memories
  inputsMemory.io.address := io.address
  inputsMemory.io.readEnable := io.readEnable
  inputsMemory.io.writeEnable := io.writeEnable
  inputsMemory.io.dataWrite := io.inputsWrite
  io.inputsRead := inputsMemory.io.dataRead

  weightsMemory.io.address := io.address
  weightsMemory.io.readEnable := io.readEnable
  io.weightsRead := weightsMemory.io.dataRead

  biasMemory.io.address := io.address
  biasMemory.io.readEnable := io.readEnable
  io.biasRead := biasMemory.io.dataRead

  // Single memories
  signsMemory.io.address := io.address
  signsMemory.io.readEnable := io.readEnable
  io.signsRead := signsMemory.io.dataRead

  fixedPointsMemory.io.address := io.address
  fixedPointsMemory.io.readEnable := io.readEnable
  io.fixedPointRead := fixedPointsMemory.io.dataRead
}
