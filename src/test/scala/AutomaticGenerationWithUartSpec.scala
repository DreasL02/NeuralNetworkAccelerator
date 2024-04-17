import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class AutomaticGenerationWithUartSpec extends AnyFreeSpec with ChiselScalatestTester {

  val clockTimeout = 200_000_000
  val frequency = 100
  val baudRate = 1
  val cyclesPerSerialBit = scala_utils.UartCoding.cyclesPerSerialBit(frequency, baudRate)
  val tenSeconds = frequency * 10

  val filepath = "ONNX Python/json/8x8.json"
  val lists = SpecToListConverter.convertSpecToLists(filepath)
  val pipelineIO = false
  val print = true

  val high = 1.U(1.W)
  val low = 0.U(1.W)

  "Should support a single byte buffer" in {
    test(new AutomaticGenerationWithUart(frequency, baudRate, lists._2, lists._3, pipelineIO, false, print)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>

      dut.clock.setTimeout(clockTimeout)
      dut.io.uartRxPin.poke(high) // UART idle signal is high

      for (i <- 0 until tenSeconds) {
        dut.io.uartTxPin.expect(high) // UART idle signal is high
        dut.clock.step(1)
      }
    }
  }
}