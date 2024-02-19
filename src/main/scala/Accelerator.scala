import communication.Encodings.Codes.none
import chisel3._
import chisel3.util.log2Ceil
import communication.{Communicator, Decoder}
import utils.Optional.optional

// Top level module for the accelerator
class Accelerator(w: Int = 8, // width of the data
                  wBig: Int = 32,
                  xDimension: Int = 4, // dimension of the matrices
                  yDimension: Int = 4, // dimension of the matrices
                  initialInputsMemoryState: Array[Array[BigInt]], // initial state of the input memory
                  initialWeightsMemoryState: Array[Array[BigInt]], // state of the weights memory
                  initialBiasMemoryState: Array[Array[BigInt]], // state of the bias memory
                  signed: Boolean = true, // signed or unsigned data
                  fixedPoint: Int = 0,
                  enableDebuggingIO: Boolean = true // enable debug signals for testing
                 ) extends Module {


  val io = IO(new Bundle {
    val incrementAddress = Input(Bool())
    val readEnable = Input(Bool())
    val writeEnable = Input(Bool())

    val dataOutW = Output(Vec(xDimension * yDimension, UInt(w.W)))
    val dataInW = Input(Vec(xDimension * yDimension, UInt(w.W)))

    val startCalculation = Input(Bool())
    val calculationDone = Output(Bool())

    // all but rxd, txd and states are debug signals
    val readDebug = optional(enableDebuggingIO, Input(Bool()))

    val debugMatrixMemory1 = optional(enableDebuggingIO, Output(Vec(xDimension * yDimension, UInt(w.W))))
    val debugMatrixMemory2 = optional(enableDebuggingIO, Output(Vec(xDimension * yDimension, UInt(w.W))))
    val debugMatrixMemory3 = optional(enableDebuggingIO, Output(Vec(xDimension * yDimension, UInt(wBig.W))))
    val debugAddress = optional(enableDebuggingIO, Output(UInt(log2Ceil(initialInputsMemoryState(0).length).W)))
  })

  // TODO: investigate if mapped correctly
  def mapResultToInput(result: Vec[Vec[UInt]]): Vec[Vec[UInt]] = { // maps the result from the accumulator to the input format for the memories
    val matrix = VecInit(Seq.fill(xDimension)(VecInit(Seq.fill(yDimension)(0.U(w.W)))))
    for (i <- 0 until xDimension) {
      for (j <- 0 until yDimension) {
        matrix(i)(j) := result(i)(yDimension - 1 - j)
      }
    }
    matrix
  }

  // TODO: investigate if mapped correctly
  def convertVecToMatrix(vector: Vec[UInt]): Vec[Vec[UInt]] = { // converts a vector to a matrix in hardware
    val matrix = VecInit(Seq.fill(xDimension)(VecInit(Seq.fill(yDimension)(0.U(w.W)))))
    for (i <- 0 until xDimension) {
      for (j <- 0 until yDimension) {
        matrix(i)(j) := vector(i * xDimension + j)
      }
    }
    matrix
  }

  // TODO: investigate if mapped correctly
  def convertMatrixToVec(matrix: Vec[Vec[UInt]]): Vec[UInt] = { // converts a matrix to a vector in hardware
    val vector = VecInit(Seq.fill(xDimension * yDimension)(0.U(w.W)))
    for (i <- 0 until xDimension) {
      for (j <- 0 until yDimension) {
        vector(i * xDimension + j) := matrix(i)(j)
      }
    }
    vector
  }

  val numberOfBytes = (w.toFloat / 8.0f).ceil.toInt // number of bytes needed to represent a w bit number

  val addressManager = Module(new AddressManager(initialInputsMemoryState(0).length))

  val memories = Module(new Memories(w, wBig, xDimension, yDimension, initialInputsMemoryState, initialWeightsMemoryState,
    initialBiasMemoryState))

  val layerFSM = Module(new LayerFSM)

  val layerCalculator = Module(new LayerCalculator(w, wBig, xDimension, yDimension, signed, fixedPoint, enableDebuggingIO))

  // Determine if address should be incremented (if either of the FSM say so)
  addressManager.io.incrementAddress := io.incrementAddress || layerFSM.io.incrementAddress

  // Connect memories to address
  memories.io.address := addressManager.io.address

  // Determine if memories should be written to (if either of the FSM say so)
  memories.io.writeEnable := layerFSM.io.writeMemory || io.writeEnable

  // Determine if memories should be read from (if either of the FSM say so or a debug signal is set)
  memories.io.readEnable := layerFSM.io.readMemory || io.readEnable || io.readDebug.getOrElse(false.B)

  // Connect up the two FSMs
  layerFSM.io.start := io.startCalculation
  io.calculationDone := layerFSM.io.finished

  // Connect up the Layer FSM and the Layer Calculator
  layerCalculator.io.load := layerFSM.io.loadBuffers
  layerFSM.io.calculatingDone := layerCalculator.io.valid

  // Connect up the Layer Calculator and the memories
  layerCalculator.io.inputs := convertVecToMatrix(memories.io.inputsRead) // map from vector to matrix, as the calculator expects a matrix
  layerCalculator.io.weights := convertVecToMatrix(memories.io.weightsRead) // map from vector to matrix, as the calculator expects a matrix
  layerCalculator.io.biases := convertVecToMatrix(memories.io.biasRead) // map from vector to matrix, as the calculator expects a matrix

  // Default values for writing to input memory
  memories.io.inputsWrite := VecInit(Seq.fill(xDimension * yDimension)(0.U(w.W)))

  when(io.writeEnable) { // the communicator is writing to the memories
    memories.io.inputsWrite := io.dataInW // write the w vectors to the memories
  }.otherwise(
    when(layerFSM.io.writeMemory) { // the layer calculator is writing to the memories
      memories.io.inputsWrite := convertMatrixToVec(mapResultToInput(layerCalculator.io.result)) // map from matrix to vector, as the memories expect a vector. Also reorder the result from the calculator to match the memory layout
    }
  )

  io.dataOutW := VecInit(Seq.fill(xDimension * yDimension)(0.U(w.W))) // pass on the byte vectors to the communicator
  // Default values for reading from input memory
  when(io.readEnable) { // the communicator is reading from the memories
    io.dataOutW := memories.io.inputsRead // map the w vectors from the memories to byte vectors for the communicator
  }

  // Debug signals
  if (enableDebuggingIO) {
    io.debugAddress.get := addressManager.io.address
    io.debugMatrixMemory1.get := memories.io.inputsRead
    io.debugMatrixMemory2.get := memories.io.weightsRead
    io.debugMatrixMemory3.get := memories.io.biasRead
  }
}
