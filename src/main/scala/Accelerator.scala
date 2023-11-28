import communication.Encodings.Codes.none
import chisel3._
import chisel3.util.log2Ceil
import communication.{Communicator, Decoder}

// Top level module for the accelerator
class Accelerator(w: Int = 8,  // width of the data
                  dimension: Int = 4,  // dimension of the matrices
                  frequency: Int,  // frequency of the uart
                  baudRate: Int,   // baud rate of the uart
                  initialInputsMemoryState: Array[Int], // initial state of the input memory
                  initialWeightsMemoryState: Array[Int], // state of the weights memory
                  initialBiasMemoryState: Array[Int], // state of the bias memory
                  initialSignsMemoryState: Array[Int], // state of the signs memory
                  initialFixedPointsMemoryState: Array[Int], // state of the fixed points memory
                  enableDebuggingIO: Boolean = true // enable debug signals for testing
                      ) extends Module {

  private def optional[T](enable: Boolean, value: T): Option[T] = { // for optional debug signals, https://groups.google.com/g/chisel-users/c/8XUcalmRp8M
    if (enable) Some(value) else None
  }

  val io = IO(new Bundle {
    // all but rxd, txd and states are debug signals
    // states can be mapped to LEDs to indicate the current state.
    val rxd = Input(Bool())

    val txd = Output(Bool())
    val states = Output(Vec(6, Bool()))

    val readDebug = optional(enableDebuggingIO, Input(Bool()))

    val debugMatrixMemory1 = optional(enableDebuggingIO, Output(Vec(dimension * dimension, UInt(w.W))))
    val debugMatrixMemory2 = optional(enableDebuggingIO, Output(Vec(dimension * dimension, UInt(w.W))))
    val debugMatrixMemory3 = optional(enableDebuggingIO, Output(Vec(dimension * dimension, UInt(w.W))))
    val address = optional(enableDebuggingIO, Output(UInt(8.W)))
    val matrixAddress = optional(enableDebuggingIO, Output(UInt(log2Ceil(initialInputsMemoryState.length).W)))
  })

  def mapResultToInput(result: Vec[Vec[UInt]]): Vec[Vec[UInt]] = { // maps the result from the accumulator to the input format for the memories
    val matrix = VecInit(Seq.fill(dimension)(VecInit(Seq.fill(dimension)(0.U(w.W)))))
    for (i <- 0 until dimension) {
      for (j <- 0 until dimension) {
        matrix(i)(j) := result(i)(dimension - 1 - j)
      }
    }
    matrix
  }

  def convertVecToMatrix(vector: Vec[UInt]): Vec[Vec[UInt]] = { // converts a vector to a matrix
    val matrix = VecInit(Seq.fill(dimension)(VecInit(Seq.fill(dimension)(0.U(w.W)))))
    for (i <- 0 until dimension) {
      for (j <- 0 until dimension) {
        matrix(i)(j) := vector(i * dimension + j)
      }
    }
    matrix
  }

  def convertMatrixToVec(matrix: Vec[Vec[UInt]]): Vec[UInt] = { // converts a matrix to a vector
    val vector = VecInit(Seq.fill(dimension * dimension)(0.U(w.W)))
    for (i <- 0 until dimension) {
      for (j <- 0 until dimension) {
        vector(i * dimension + j) := matrix(i)(j)
      }
    }
    vector
  }

  val numberOfBytes = (w.toFloat / 8.0f).ceil.toInt // number of bytes needed to represent a w bit number

  val addressManager = Module(new AddressManager(dimension, initialInputsMemoryState.length, initialFixedPointsMemoryState.length))

  val communicator = Module(new Communicator(dimension * dimension * numberOfBytes, frequency, baudRate))

  val memories = Module(new Memories(w, dimension, initialInputsMemoryState, initialWeightsMemoryState,
    initialBiasMemoryState, initialSignsMemoryState, initialFixedPointsMemoryState))

  val layerFSM = Module(new LayerFSM)

  val layerCalculator = Module(new LayerCalculator(w, dimension))

  // for mapping between w and byte in the interface between memories and communicator
  val byteIntoVectorCollector = Module(new ByteIntoVectorCollector(w, dimension))
  val vectorIntoByteSplitter = Module(new VectorIntoByteSplitter(w, dimension))

  // Pass on UART pints
  communicator.io.uartRxPin := io.rxd
  io.txd := communicator.io.uartTxPin

  // Determine if address should be incremented (if either of the FSM say so)
  addressManager.io.incrementAddress := communicator.io.incrementAddress || layerFSM.io.incrementAddress

  // Connect memories to address
  memories.io.matrixAddress := addressManager.io.matrixAddress
  memories.io.vectorAddress := addressManager.io.vectorAddress

  // Determine if memories should be written to (if either of the FSM say so)
  memories.io.writeEnable := layerFSM.io.writeMemory || communicator.io.writeEnable

  // Determine if memories should be read from (if either of the FSM say so or a debug signal is set)
  memories.io.readEnable := layerFSM.io.readMemory || communicator.io.readEnable || io.readDebug.getOrElse(false.B)

  // Emit the states in the communicator FSM
  io.states := communicator.io.states

  // Connect up the two FSMs
  layerFSM.io.start := communicator.io.startCalculation
  communicator.io.calculationDone := layerFSM.io.finished

  // Connect up the Layer FSM and the Layer Calculator
  layerCalculator.io.load := layerFSM.io.loadBuffers
  layerFSM.io.calculatingDone := layerCalculator.io.valid

  // Connect up the Layer Calculator and the memories
  layerCalculator.io.inputs := convertVecToMatrix(memories.io.inputsRead) // map from vector to matrix, as the calculator expects a matrix
  layerCalculator.io.weights := convertVecToMatrix(memories.io.weightsRead) // map from vector to matrix, as the calculator expects a matrix
  layerCalculator.io.biases := convertVecToMatrix(memories.io.biasRead) // map from vector to matrix, as the calculator expects a matrix
  layerCalculator.io.signed := memories.io.signsRead(0) === 1.U //bool conversion, as the calculator expects a bool
  layerCalculator.io.fixedPoint := memories.io.fixedPointRead // pass on the fixed point value

  // Default values for writing to input memory
  byteIntoVectorCollector.io.input := VecInit(Seq.fill(dimension * dimension * numberOfBytes)(0.U(8.W)))
  memories.io.inputsWrite := VecInit(Seq.fill(dimension * dimension)(0.U(w.W)))

  when(communicator.io.writeEnable) { // the communicator is writing to the memories
    byteIntoVectorCollector.io.input := communicator.io.dataOut  // map the byte vectors from the communicator to w vectors for the memories
    memories.io.inputsWrite := byteIntoVectorCollector.io.output // write the w vectors to the memories
  }.otherwise(
    when(layerFSM.io.writeMemory) { // the layer calculator is writing to the memories
      memories.io.inputsWrite := convertMatrixToVec(mapResultToInput(layerCalculator.io.result)) // map from matrix to vector, as the memories expect a vector. Also reorder the result from the calculator to match the memory layout
    }
  )

  // Default values for reading from input memory
  vectorIntoByteSplitter.io.input := VecInit(Seq.fill(dimension * dimension)(0.U(w.W)))
  when(communicator.io.readEnable) { // the communicator is reading from the memories
    vectorIntoByteSplitter.io.input := memories.io.inputsRead // map the w vectors from the memories to byte vectors for the communicator
  }
  communicator.io.dataIn := vectorIntoByteSplitter.io.output // pass on the byte vectors to the communicator

  // Debug signals
  if (enableDebuggingIO) {
    io.address.get := addressManager.io.vectorAddress
    io.debugMatrixMemory1.get := memories.io.inputsRead
    io.debugMatrixMemory2.get := memories.io.weightsRead
    io.debugMatrixMemory3.get := memories.io.biasRead
    io.matrixAddress.get := addressManager.io.matrixAddress
  }
}
