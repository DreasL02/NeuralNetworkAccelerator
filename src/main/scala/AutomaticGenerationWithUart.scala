import chisel3._
import communication.Communicator

class AutomaticGenerationWithUart(
                                   frequency: Int,
                                   baudRate: Int,
                                   listOfNodes: List[Any],
                                   connectionList: List[List[Int]],
                                   pipelineIO: Boolean = false,
                                   enableDebuggingIO: Boolean = true,
                                   printing: Boolean = true,
                                   matrixByteSize: Int,
                                 ) extends Module {

  val io = IO(new Bundle {
    val uartRxPin = Input(UInt(1.W))
    val uartTxPin = Output(UInt(1.W))
  })

  private val communicator = Module(new Communicator(matrixByteSize, frequency, baudRate))
  communicator.io.uartRxPin := io.uartRxPin
  io.uartTxPin := communicator.io.uartTxPin

  private val automaticGeneration = Module(new AutomaticGeneration(listOfNodes, connectionList, pipelineIO, enableDebuggingIO, printing))

  val imageSize = 8

  for (i <- 0 until imageSize) {
    for (j <- 0 until imageSize) {
      val flatIndex = i * imageSize + j
      automaticGeneration.io.inputChannel.bits(0)(0)(i)(j) := communicator.io.dataOut(flatIndex)
    }
  }

  communicator.io.dataIn := automaticGeneration.io.outputChannel.bits(0)(0)(0)

  automaticGeneration.io.outputChannel.ready := communicator.io.readEnable
  automaticGeneration.io.inputChannel.valid := communicator.io.writeEnable
  communicator.io.calculationDone := automaticGeneration.io.outputChannel.valid
  automaticGeneration.io.inputChannel.valid := communicator.io.startCalculation
}
