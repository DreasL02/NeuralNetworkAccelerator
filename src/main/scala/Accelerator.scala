import communication.Encodings.Codes.none
import chisel3._
import chisel3.util.log2Ceil
import communication.{Communicator, Decoder}

class Accelerator(w: Int = 8, dimension: Int = 4,
                  initialInputsMemoryState: Array[Int], initialWeightsMemoryState: Array[Int],
                  initialBiasMemoryState: Array[Int], initialSignsMemoryState: Array[Int],
                  initialFixedPointsMemoryState: Array[Int]) extends Module {
  val io = IO(new Bundle {
    val rxd = Input(Bool())
    val txd = Output(Bool())
    val result = Output(Vec(dimension, Vec(dimension, UInt(w.W))))
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
    val vector = VecInit(Seq.fill(dimension * dimension)(0.U(w.W)))
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

  val communicator = Module(new Communicator())

  val decoder = Module(new Decoder())

  decoder.io.opcode := communicator.io.opcode

  addressManager.io.incrementMatrixAddress := controller.io.incrementAddress
  addressManager.io.incrementVectorAddress := controller.io.incrementAddress

  memories.io.dataAddress := addressManager.io.matrixAddress
  memories.io.configAddress := addressManager.io.vectorAddress
  memories.io.writeAddress := addressManager.io.matrixAddress

  mmac.io.inputs := convertVecToMatrix(memories.io.dataRead)
  mmac.io.weights := convertVecToMatrix(memories.io.weightsRead)
  mmac.io.biases := convertVecToMatrix(memories.io.biasRead)
  mmac.io.signed := memories.io.signsRead
  mmac.io.fixedPoint := memories.io.fixedPointRead

  //TODO
  controller.io.receivedMessage := false.B
  controller.io.inputsStored := false.B
  controller.io.transmissionDone := false.B
  controller.io.readingDone := false.B
  controller.io.calculatingDone := mmac.io.valid
  controller.io.writingDone := false.B
  controller.io.addressChanged := false.B
  controller.io.decodingCode := decoder.io.decodingCode

  mmac.io.loadInputs := controller.io.loadBuffers
  mmac.io.loadWeights := controller.io.loadBuffers
  mmac.io.loadBiases := controller.io.loadBiases

  memories.io.read := controller.io.readMemory
  memories.io.write := controller.io.writeMemory

  memories.io.dataWrite := convertMatrixToVec(mmac.io.result)

  io.result := mmac.io.result
}
