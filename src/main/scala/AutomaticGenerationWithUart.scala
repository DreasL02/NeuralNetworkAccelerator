import chisel3._
import communication.Communicator
import communication.chisel.lib.uart.{BufferedUartRxForTestingOnly, BufferedUartTxForTestingOnly}

class AutomaticGenerationWithUart(
                                   frequency: Int,
                                   baudRate: Int,
                                   listOfNodes: List[Any],
                                   connectionList: List[List[Int]],
                                   printing: Boolean = true,
                                   matrixByteSize: Int,
                                 ) extends Module {

  val io = IO(new Bundle {
    val uartRxPin = Input(UInt(1.W))
    val uartTxPin = Output(UInt(1.W))
    val calculatorReady = Output(Bool())
    val calculatorValid = Output(Bool())
    val uartRxValid = Output(Bool())
  })

  val imageSize = 8
  val imageByteSize = 8 * 8 // 8x8 image with 2 bytes per pixel
  val responseByteSize = 10 // 10 value response with 2 bytes per value

  val calculator = Module(new AutomaticGeneration(listOfNodes, connectionList, printing))

  val bufferedUartRx = Module(new BufferedUartRxForTestingOnly(frequency, baudRate, imageByteSize))
  bufferedUartRx.io.rxd := io.uartRxPin
  calculator.io.inputChannel.valid := bufferedUartRx.io.outputChannel.valid
  bufferedUartRx.io.outputChannel.ready := calculator.io.inputChannel.ready
  for (i <- 0 until imageSize) {
    for (j <- 0 until imageSize) {
      val flatIndex = i * imageSize + j
      //calculator.io.inputChannel.bits(0)(0)(i)(j) := 0.U
      calculator.io.inputChannel.bits(0)(0)(i)(j) := bufferedUartRx.io.outputChannel.bits(flatIndex)
    }
  }

  val bufferedUartTx = Module(new BufferedUartTxForTestingOnly(frequency, baudRate, 1))
  io.uartTxPin := bufferedUartTx.io.txd
  bufferedUartTx.io.inputChannel.valid := calculator.io.outputChannel.valid
  calculator.io.outputChannel.ready := bufferedUartTx.io.inputChannel.ready
  bufferedUartTx.io.inputChannel.bits(0) := "b01010101".U

  io.calculatorReady := calculator.io.inputChannel.ready
  io.calculatorValid := calculator.io.outputChannel.valid
  io.uartRxValid := bufferedUartRx.io.outputChannel.valid
}
