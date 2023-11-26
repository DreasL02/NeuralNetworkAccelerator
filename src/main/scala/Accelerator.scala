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
    communicator.io.dataIn(i) := 0.U //default
    when(communicator.io.readEnable) {
      communicator.io.dataIn(i) := memory(addressManager.io.matrixAddress + i.U)
    }
  }

  io.address := addressManager.io.vectorAddress
  io.ready := communicator.io.ready

  for (i <- 0 until dimension * dimension) {
    io.matrixMemory(i) := memory(addressManager.io.matrixAddress + i.U)
  }

  // TODO: couple up to layer FSM
  communicator.io.calculationDone := false.B


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

    */


}
