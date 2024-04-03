import chisel3._
import chiseltest._
import communication.Communicator
import org.scalatest.freespec.AnyFreeSpec

class CommunicatorSpec extends AnyFreeSpec with ChiselScalatestTester {

  val clockTimeout = 200_000_000
  val frequency = 5000 * 2
  val baudRate = 10
  val cyclesPerSerialBit = scala_utils.UartCoding.cyclesPerSerialBit(frequency, baudRate)
  val tenSeconds = frequency * 10
  val uartFrameSize = 11

  val high = 1.U(1.W)
  val low = 0.U(1.W)

  "Should ..." in {
    test(new Communicator(10, frequency, baudRate)) { dut =>

      dut.clock.setTimeout(clockTimeout)

      dut.io.startCalculation.expect(false.B)

    }
  }


}
