import chisel3._
import chisel3.util.log2Ceil

class Accelerator(w: Int = 8, dimension: Int = 4,
                  initialDataMemoryState: Array[Int], initialWeightsMemoryState: Array[Int],
                  initialBiasMemoryState: Array[Int], initialConfigMemoryState: Array[Int]) extends Module {
  val io = IO(new Bundle {
  })

  def convertVecToMatrix(vector: Vec[UInt]): Vec[Vec[UInt]] = {
    val matrix = VecInit(Seq.fill(dimension)(VecInit(Seq.fill(dimension)(0.U(w.W)))))
    for (i <- 0 until dimension) {
      for (j <- 0 until dimension) {
        matrix(i)(j) := vector(i * dimension + j)
      }
    }
    matrix
  }

  def convertMatrixToVec(matrix: Vec[Vec[UInt]]): Vec[UInt] = {
    val vector = VecInit(Seq.fill(dimension)(0.U(w.W)))
    for (i <- 0 until dimension) {
      for (j <- 0 until dimension) {
        vector(i * dimension + j) := matrix(i)(j)
      }
    }
    vector
  }

  val memories = Module(new Memories(w, dimension, initialDataMemoryState, initialWeightsMemoryState,
    initialBiasMemoryState, initialConfigMemoryState))

  val mmu = Module(new MatrixMultiplicationUnit(w, dimension))

  //missing address assignment

  mmu.io.inputs := convertVecToMatrix(memories.io.dataRead)
  mmu.io.weights := convertVecToMatrix(memories.io.weightsRead)
  mmu.io.biases := convertVecToMatrix(memories.io.biasRead)
  mmu.io.signed := memories.io.configRead(0)
  mmu.io.fixedPoint := memories.io.configRead(log2Ceil(w) + 1, 1)

  memories.io.write := mmu.io.valid
  memories.io.dataWrite := convertMatrixToVec(mmu.io.result)
}
