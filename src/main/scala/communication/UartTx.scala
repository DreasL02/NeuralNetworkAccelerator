/*
 *
 * Modified version of
 * https://github.com/freechipsproject/ip-contributions/blob/master/src/main/scala/chisel/lib/uart/Uart.scala
 * by Martin Schoeberl
 *
 */

package communication

package chisel.lib.uart

import chisel3._
import chisel3.util._

// Refactored version of Chisel ip-contribution uart by Martin Schoeberl
// https://github.com/freechipsproject/ip-contributions/blob/master/src/main/scala/chisel/lib/uart/Uart.scala
// (visited 08-04-2024)

class UartTx(frequency: Int, baudRate: Int) extends Module {
  val io = IO(new Bundle {
    val txd = Output(UInt(1.W))
    val inputChannel = Flipped(new DecoupledIO(UInt(8.W)))
  })

  val CYCLES_PER_SERIAL_BIT = ((frequency + baudRate / 2) / baudRate - 1).asUInt
  println("CYCLES_PER_SERIAL_BIT: " + CYCLES_PER_SERIAL_BIT)
  val SEQUENCE_LENGTH = 1 + 8 + 2 // one start bit, 8 data bits, two stop bits
  val ELEVEN_HIGH_BITS = 0x7ff.U

  val shiftReg = RegInit(ELEVEN_HIGH_BITS)
  val cyclesCountReg = RegInit(0.U(20.W))
  val bitsIndexReg = RegInit(0.U(4.W))

  io.inputChannel.ready := (cyclesCountReg === 0.U) && (bitsIndexReg === 0.U)
  io.txd := shiftReg(0)

  when(cyclesCountReg === 0.U) {
    cyclesCountReg := CYCLES_PER_SERIAL_BIT

    when(bitsIndexReg =/= 0.U) {
      val shift = shiftReg >> 1
      shiftReg := 1.U ## shift(9, 0)
      bitsIndexReg := bitsIndexReg - 1.U
    }.otherwise {
      // cyclesCountReg is 0 and bitsIndexReg is 0, so we are ready to send a new byte.
      // If the input channel is valid, we send the daya from inputChannel. Otherwise, we send 11 idle bits.
      when(io.inputChannel.valid) {
        // two stop bits, data, one start bit
        shiftReg := "b11".U ## io.inputChannel.bits ## "b0".U
        bitsIndexReg := SEQUENCE_LENGTH.U
      }.otherwise {
        shiftReg := ELEVEN_HIGH_BITS
      }
    }

  }.otherwise {
    cyclesCountReg := cyclesCountReg - 1.U
  }
}
