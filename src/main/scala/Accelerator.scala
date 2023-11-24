import communication.Encodings.Codes.none
import chisel3._
import chisel3.util.log2Ceil
import communication.{Communicator, Decoder}

class Accelerator(w: Int = 8, dimension: Int = 4, frequency: Int, baudRate: Int,
                  initialInputsMemoryState: Array[Byte]
                  /*,
                                    initialWeightsMemoryState: Array[Int],
                                    initialBiasMemoryState: Array[Int],
                                    initialSignsMemoryState: Array[Int],
                                    initialFixedPointsMemoryState: Array[Int]*/
                 ) extends Module {
  val io = IO(new Bundle {
    val rxd = Input(Bool())
    val txd = Output(Bool())
    val ready = Output(Bool())
    val matrixMemory = Output(Vec(dimension * dimension, UInt(8.W)))
    val address = Output(UInt(8.W))
  })

  val addressManager = Module(new AddressManager(dimension, initialInputsMemoryState.length, 8))

  val communicator = Module(new Communicator(dimension * dimension * (w / 8), frequency, baudRate))

  val memory = RegInit(VecInit(initialInputsMemoryState.toIndexedSeq.map(_.S(w.W).asUInt)))

  communicator.io.uartRxPin := io.rxd
  io.txd := communicator.io.uartTxPin

  addressManager.io.incrementAddress := communicator.io.incrementAddress

  for (i <- 0 until dimension * dimension) {
    when(communicator.io.writeEnable) {
      memory(addressManager.io.matrixAddress + i.U) := communicator.io.dataOut(i)
    }
  }

  io.address := addressManager.io.vectorAddress
  io.ready := communicator.io.ready

  for (i <- 0 until dimension * dimension) {
    io.matrixMemory(i) := memory(addressManager.io.matrixAddress + i.U)
  }

  /*
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
    */


  /*
  memories.io.dataAddress := addressManager.io.matrixAddress
  memories.io.configAddress := addressManager.io.vectorAddress
  memories.io.writeAddress := addressManager.io.matrixAddress

  mmac.io.inputs := convertVecToMatrix(memories.io.dataRead)
  mmac.io.weights := convertVecToMatrix(memories.io.weightsRead)
  mmac.io.biases := convertVecToMatrix(memories.io.biasRead)
  mmac.io.signed := memories.io.signsRead
  mmac.io.fixedPoint := memories.io.fixedPointRead

  // TODO

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
*/
}
