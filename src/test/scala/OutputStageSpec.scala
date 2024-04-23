
import chisel3.Module
import chiseltest._
import onnx.SpecToListConverter
import org.scalatest.freespec.AnyFreeSpec
import stages.OutputStageDebugger

class OutputStageSpec extends AnyFreeSpec with ChiselScalatestTester {

  val wIn = 32
  val wOut = 1
  val inputShape = (1, 1, 1, 1)
  val outputShape = (1, 1, 1, 1)
  val baudRate = 1
  val frequency = 2

  "Outputstage" in {
    test(new OutputStageDebugger(wIn, wOut, inputShape, outputShape, baudRate, frequency)) { dut =>

      dut.io.inputChannel.valid.poke(false)
      for (i <- 0 until 150) {
        dut.io.txdPin.expect(1)
        dut.clock.step()
      }

      dut.io.inputChannel.bits(0)(0)(0)(0).poke(255)
      dut.io.inputChannel.valid.poke(true)
      dut.clock.step()

      dut.io.reshaperInputChannelValid.expect(true)
      dut.io.reshaperInputChannelValid.expect(true)

      dut.io.byteConverterInputChannelValid.expect(true)
      dut.io.byteConverterOutputChannelValid.expect(true)

      dut.io.reshaperInputChannelBits(0)(0)(0)(0).expect(255)
      dut.io.reshaperOutputChannelBits(0)(0)(0)(0).expect(255)
      dut.io.byteConverterInputChannelBits(0).expect(255)



      dut.io.inputChannel.valid.poke(false)

      while (dut.io.txdPin.peekInt() == 1) {
        dut.clock.step()
      }

      for (i <- 0 until 50) {
        println(dut.io.txdPin.peekInt())
        dut.clock.step(2)
      }

    }
  }

}
