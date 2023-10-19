import chisel3._
import chisel3.util.log2Ceil

class Accelerator(w: Int = 8, dimension: Int = 4,
                  initialInputsMemoryState: Array[Int], initialWeightsMemoryState: Array[Int],
                  initialBiasMemoryState: Array[Int], initialSignsMemoryState: Array[Int],
                  initialFixedPointsMemoryState: Array[Int]) extends Module {
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

  val memories = Module(new Memories(w, dimension, initialInputsMemoryState, initialWeightsMemoryState,
    initialBiasMemoryState, initialSignsMemoryState, initialFixedPointsMemoryState))

  val mmac = Module(new MatrixMultiplicationUnit(w, dimension))

  val controller = Module(new Controller())

  val addressManager = Module(new AddressManager(dimension, initialInputsMemoryState.length, initialWeightsMemoryState.length))

  //TODO: missing address assignment
  memories.io.dataAddress := addressManager.matrixAddressReg
  memories.io.configAddress := addressManager.vectorAddressReg

  mmac.io.inputs := convertVecToMatrix(memories.io.dataRead)
  mmac.io.weights := convertVecToMatrix(memories.io.weightsRead)
  mmac.io.biases := convertVecToMatrix(memories.io.biasRead)
  mmac.io.signed := memories.io.signsRead
  mmac.io.fixedPoint := memories.io.fixedPointRead

  memories.io.write := mmac.io.valid
  memories.io.dataWrite := convertMatrixToVec(mmac.io.result)
}
