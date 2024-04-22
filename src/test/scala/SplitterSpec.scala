import chisel3._
import chiseltest._
import module_utils.{ByteCollector, ByteIntoFlatVectorCollector, ByteIntoVectorCollector, ByteSplitter, VectorIntoByteSplitter}
import org.scalatest.freespec.AnyFreeSpec
import scala_utils.UartCoding.padLeft

class SplitterSpec extends AnyFreeSpec with ChiselScalatestTester {
  "Should not split a byte" in {
    test(new ByteSplitter(8)) { dut =>
      val input = 5.U
      dut.io.input.poke(input)
      dut.io.output(0).expect(input.asUInt)
    }
  }

  "Should split 16-bit value into two bytes" in {
    test(new ByteSplitter(16)) { dut =>
      val input = 7.U
      // 7 = 0b0000000000000111
      // 0b00000111 = 7
      // 0b00000000 = 0
      dut.io.input.poke(input)
      dut.io.output(0).expect(7) // lsb in first byte
      dut.io.output(1).expect(0) // msb in second byte
    }
  }

  "Should split 16-bit value into two bytes 2" in {
    test(new ByteSplitter(16)) { dut =>
      val input = 2132.U
      // 2132 = 0b100001010100
      // 0b01010100 = 84
      // 0b00001000 = 8
      dut.io.input.poke(input)
      dut.io.output(0).expect(84) // lsb in first byte
      dut.io.output(1).expect(8) // msb in second byte
    }
  }

  "Should split 32-bit value into four bytes" in {
    test(new ByteSplitter(32)) { dut =>
      val input = 21323227.U
      // 21323227 = 0b1 01000101 01011101 11011011
      // 0b11011011 = 219
      // 0b01011101 = 93
      // 0b01000101 = 69
      // 0b00000001 = 1


      dut.io.input.poke(input)
      dut.io.output(0).expect(219) // lsb
      dut.io.output(1).expect(93)
      dut.io.output(2).expect(69)
      dut.io.output(3).expect(1) // msb
    }
  }

  "Should collect four bytes into a 32-bit value" in {
    test(new ByteCollector(32)) { dut =>
      val input1 = 219.U
      val input2 = 93.U
      val input3 = 69.U
      val input4 = 1.U

      dut.io.input(0).poke(input1) // lsb
      dut.io.input(1).poke(input2)
      dut.io.input(2).poke(input3)
      dut.io.input(3).poke(input4) // msb

      dut.io.output.expect(21323227.U)
    }
  }

  "Should split 16-bit vector into 2 bytes each" in {
    test(new VectorIntoByteSplitter(16, 2)) { dut =>
      val input = Seq(7.U, 2132.U, 227.U, 1.U)
      dut.io.input.zip(input).foreach { case (pin, value) => pin.poke(value) }
      dut.io.output(0).expect(7) // lsb in first byte
      dut.io.output(1).expect(0) // msb in second byte
      dut.io.output(2).expect(84) // lsb in first byte
      dut.io.output(3).expect(8) // msb in second byte
      dut.io.output(4).expect(227) // lsb in first byte
      dut.io.output(5).expect(0) // msb in second byte
      dut.io.output(6).expect(1) // lsb in first byte
      dut.io.output(7).expect(0) // msb in second byte
    }
  }

  "Should collect 2 bytes each into a 16-bit vector" in {
    test(new ByteIntoVectorCollector(16, 2)) { dut =>
      val input = Seq(7.U, 0.U, 84.U, 8.U, 227.U, 0.U, 1.U, 0.U)
      dut.io.input.zip(input).foreach { case (pin, value) => pin.poke(value) }
      dut.io.output(0).expect(7) // two first bytes (7, 0)
      dut.io.output(1).expect(2132) // two next bytes (84, 8)
      dut.io.output(2).expect(227) // two next bytes (227, 0)
      dut.io.output(3).expect(1) // two next bytes (1, 0)
    }
  }

  "Should collect 9 bytes (total 72 bits) into 8 9-bit values (also 72 bits)" in {
    val outputUnit = 9
    val byteCount = 9

    test(new ByteIntoFlatVectorCollector(byteCount, outputUnit)) { dut =>

      val input = Seq(7, 0, 84, 8, 227, 0, 1, 0, 11)
      assert(input.length == byteCount)
      //val input = Seq(0, 0, 0, 0, 0, 0, 0, 0, 0)

      for (i <- input.indices) {
        dut.io.inputChannel.bits(i).poke(input(i).U(8.W))
      }

      // TODO: IVAN
      //val inputAsBinaryString = input.map(padLeft()).mkString(" ")
      //println(s"inputAsBinaryString:  $inputAsBinaryString")
      //println(s"count of ones: ${inputAsBinaryString.count(_ == '1')}")

      val outputAsBinaryString = dut.io.outputChannel.bits.map(_.peekInt().toInt.toBinaryString).mkString(" ")
      println(s"outputAsBinaryString: $outputAsBinaryString")
      println(s"count of ones: ${outputAsBinaryString.count(_ == '1')}")


      //dut.io.output(0).expect(7) // two first bytes (7, 0)
      //dut.io.output(1).expect(2132) // two next bytes (84, 8)
      //dut.io.output(2).expect(227) // two next bytes (227, 0)
      //dut.io.output(3).expect(1) // two next bytes (1, 0)
    }
  }



}
