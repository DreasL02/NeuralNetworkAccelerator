import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import scala_utils.FixedPointConversion.floatToFixed

import scala.collection.mutable.ArrayBuffer

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

  val parameters = lists._1
  val width = parameters.w
  val fixedPoint = parameters.fixedPoint
  val signed = parameters.signed

  val high = 1.U(1.W)
  val low = 0.U(1.W)

  val testNum = 0
  val inputFileName = "ONNX Python/digits_8x8/%d.txt".format(testNum)
  val flatData = scala.io.Source.fromFile(inputFileName).getLines().map(_.toFloat).toArray
  val fixedFlatData = flatData.map(i => floatToFixed(i, fixedPoint, width, signed))


  println("width: " + width)

  val minBytePerNumber = (width.toFloat / 8.0f).ceil.toInt
  println("minBytePerNumber: " + minBytePerNumber)
  val bytesPerMatrix = minBytePerNumber * 8 * 8


  val bytes = ArrayBuffer[Byte]()

  // loop through fixedFlatData
  for (fixedPointValue <- fixedFlatData) {
    for (i <- 0 until minBytePerNumber) {
      val byte = ((fixedPointValue >> (i * 8)) & 0xFF).toByte
      bytes.append(byte)
    }
  }

  println("values: " + fixedFlatData.length)
  println("bytes: " + bytes.length)




  val encodedBits = scala_utils.UartCoding.encodeBytesToUartBits(bytes.toArray)

  println(fixedFlatData.mkString(" "))
  println(encodedBits.mkString(""))



  "Should support a single byte buffer" in {
    test(new AutomaticGenerationWithUart(frequency, baudRate, lists._2, lists._3, pipelineIO, false, print, bytesPerMatrix)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>

      dut.clock.setTimeout(clockTimeout)
      dut.io.uartRxPin.poke(high) // UART idle signal is high

      for (i <- 0 until tenSeconds) {
        dut.io.uartTxPin.expect(high) // UART idle signal is high
        dut.clock.step(1)
      }


      val bitCount = encodedBits.length
      for (i <- 0 until bitCount) {
        val bit = encodedBits(i) - '0'
        dut.io.uartRxPin.poke(bit.U)
        dut.clock.step(cyclesPerSerialBit)
      }

      while (dut.io.uartTxPin.peek() == high) {
        dut.clock.step(1)
      }

      val buffer = ArrayBuffer[BigInt]()
      buffer.append(1)
      val outputBitCount = 10*2*11+50
      for (i <- 0 until outputBitCount) {
        buffer.append(dut.io.uartRxPin.peek().litValue)
        dut.clock.step(cyclesPerSerialBit)
      }

      println(buffer.mkString(""))


    }
  }
}