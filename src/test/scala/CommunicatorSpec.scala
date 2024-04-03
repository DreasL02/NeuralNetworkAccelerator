import chisel3._
import chiseltest._
import communication.Communicator
import org.scalatest.freespec.AnyFreeSpec
import scala_utils.UartCoding
import scala.collection.mutable.ListBuffer

class CommunicatorSpec extends AnyFreeSpec with ChiselScalatestTester {

  val clockTimeout = 200_000_000
  val frequency = 5000 * 2
  val baudRate = 10
  val cyclesPerSerialBit = scala_utils.UartCoding.cyclesPerSerialBit(frequency, baudRate)
  val tenSeconds = frequency * 10
  val uartFrameSize = 11

  val high = 1.U(1.W)
  val low = 0.U(1.W)

  val matrixByteSize = 10

  "FSM should operate correctly" in {
    test(new Communicator(matrixByteSize, frequency, baudRate)) { dut =>

      dut.clock.setTimeout(clockTimeout)
      dut.io.startCalculation.expect(false.B)

      val matrixBytes = Array[Byte](1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
      val bitsToSend = UartCoding.encodeBytesToUartBits(matrixBytes)

      bitsToSend.foreach { bit =>
        val bitAsBigInt = BigInt(bit - 48)
        dut.io.uartRxPin.poke(bitAsBigInt.U(1.W))
        dut.clock.step(cyclesPerSerialBit)
      }

      dut.io.startCalculation.expect(true.B)
      dut.io.calculationDone.poke(true.B)

      val simMatrixOut = Array[Byte](11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
      for (i <- 0 until matrixByteSize) {
        dut.io.dataIn(i).poke(simMatrixOut(i).U)
      }

      dut.clock.step(1)
      val outputLength = matrixByteSize * uartFrameSize
      val uartOutputBuffer = ListBuffer[BigInt]()
      for (i <- 0 until outputLength) {
        uartOutputBuffer.append(dut.io.uartTxPin.peekInt())
        dut.clock.step(cyclesPerSerialBit)
      }

      UartCoding.decodeUartBitsToByteArray(uartOutputBuffer.toArray, 8).foreach(println)
    }
  }


}
