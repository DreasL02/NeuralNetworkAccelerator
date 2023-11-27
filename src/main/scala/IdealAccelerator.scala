import communication.Encodings.Codes.none
import chisel3._
import chisel3.util.log2Ceil
import communication.{Communicator, Decoder}

class IdealAccelerator(w: Int = 8, dimension: Int = 4, frequency: Int, baudRate: Int,
                       initialInputsMemoryState: Array[Int],
                       initialWeightsMemoryState: Array[Int],
                       initialBiasMemoryState: Array[Int],
                       initialSignsMemoryState: Array[Int],
                       initialFixedPointsMemoryState: Array[Int]
                      ) extends Module {
  val io = IO(new Bundle {
    // all but rxd and txd are debug signals
    // ready could be mapped to a LED perhaps?
    val rxd = Input(Bool())
    val readDebug = Input(Bool())
    val forceDebug = Input(Bool())

    val txd = Output(Bool())
    val ready = Output(Bool())
    val debugMatrixMemory1 = Output(Vec(dimension * dimension, UInt(w.W)))
    val debugMatrixMemory2 = Output(Vec(dimension * dimension, UInt(w.W)))
    val debugMatrixMemory3 = Output(Vec(dimension * dimension, UInt(w.W)))
    val address = Output(UInt(8.W))
    val matrixAddress = Output(UInt(log2Ceil(initialInputsMemoryState.length).W))
  })

  def mapResultToInput(result: Vec[Vec[UInt]]): Vec[Vec[UInt]] = {
    val matrix = VecInit(Seq.fill(dimension)(VecInit(Seq.fill(dimension)(0.U(w.W)))))
    for (i <- 0 until dimension) {
      for (j <- 0 until dimension) {
        matrix(i)(j) := result(i)(dimension - 1 - j)
      }
    }
    matrix
  }

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

  val addressManager = Module(new AddressManager(dimension, initialInputsMemoryState.length, initialFixedPointsMemoryState.length))

  val communicator = Module(new Communicator(dimension * dimension * (w.toFloat / 8.0f).ceil.toInt, frequency, baudRate))

  val memories = Module(new Memories(w, dimension, initialInputsMemoryState, initialWeightsMemoryState,
    initialBiasMemoryState, initialSignsMemoryState, initialFixedPointsMemoryState))

  val layerFSM = Module(new LayerFSM)

  val layerCalculator = Module(new LayerCalculator(w, dimension))

  val byteIntoVectorCollector = Module(new ByteIntoVectorCollector(w, dimension))
  val vectorIntoByteSplitter = Module(new VectorIntoByteSplitter(w, dimension))

  communicator.io.uartRxPin := io.rxd
  io.txd := communicator.io.uartTxPin

  addressManager.io.incrementAddress := communicator.io.incrementAddress || layerFSM.io.incrementAddress

  //memories.io.writeEnable := communicator.io.writeEnable || layerFSM.io.writeMemory || io.forceDebug
  memories.io.writeEnable := layerFSM.io.writeMemory || communicator.io.writeEnable
  memories.io.readEnable := layerFSM.io.readMemory || communicator.io.readEnable || io.readDebug

  io.address := addressManager.io.vectorAddress
  io.ready := communicator.io.ready

  // layerFSM and communicator
  layerFSM.io.start := communicator.io.startCalculation
  communicator.io.calculationDone := layerFSM.io.finished

  // layerFSM and layerCalculator
  layerCalculator.io.load := layerFSM.io.loadBuffers

  layerFSM.io.calculatingDone := layerCalculator.io.valid

  // layerCalculator and memories
  layerCalculator.io.inputs := convertVecToMatrix(memories.io.inputsRead)
  layerCalculator.io.weights := convertVecToMatrix(memories.io.weightsRead)
  layerCalculator.io.biases := convertVecToMatrix(memories.io.biasRead)
  layerCalculator.io.signed := memories.io.signsRead(0) === 1.U //bool conversion
  layerCalculator.io.fixedPoint := memories.io.fixedPointRead

  memories.io.inputsWrite := VecInit(Seq.fill(dimension * dimension)(0.U(w.W)))
  when(communicator.io.writeEnable) {
    byteIntoVectorCollector.io.input := communicator.io.dataOut
    memories.io.inputsWrite := byteIntoVectorCollector.io.output
  }.otherwise(
    when(layerFSM.io.writeMemory) {
      memories.io.inputsWrite := convertMatrixToVec(mapResultToInput(layerCalculator.io.result))
    }
  )
  vectorIntoByteSplitter.io.input := VecInit(Seq.fill(dimension * dimension)(0.U(w.W)))
  when(communicator.io.readEnable) {
    vectorIntoByteSplitter.io.input := memories.io.inputsRead
  }
  communicator.io.dataIn := vectorIntoByteSplitter.io.output


  io.debugMatrixMemory1 := memories.io.inputsRead
  io.debugMatrixMemory2 := memories.io.weightsRead
  //io.debugMatrixMemory2 := memories.io.inputsWrite
  io.debugMatrixMemory3 := memories.io.biasRead
  //io.debugMatrixMemory3 := convertMatrixToVec(layerCalculator.io.result)
  io.matrixAddress := addressManager.io.matrixAddress

  // Address and memories
  memories.io.matrixAddress := addressManager.io.matrixAddress
  memories.io.vectorAddress := addressManager.io.vectorAddress
}
