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

class UartIO extends DecoupledIO(UInt(8.W))

class Tx(frequency: Int, baudRate: Int) extends Module {
  val io = IO(new Bundle {
    val txd = Output(UInt(1.W))
    val channel = Flipped(new UartIO())
  })

  val CYCLES_PER_SERIAL_BIT = ((frequency + baudRate / 2) / baudRate - 1).asUInt
  val SEQUENCE_LENGTH = 1 + 8 + 2 // one start bit, 8 data bits, two stop bits
  val ELEVEN_HIGH_BITS = 0x7ff.U

  val shiftReg = RegInit(ELEVEN_HIGH_BITS)
  val cyclesCountReg = RegInit(0.U(20.W))
  val bitsIndexReg = RegInit(0.U(4.W))

  io.channel.ready := (cyclesCountReg === 0.U) && (bitsIndexReg === 0.U)
  io.txd := shiftReg(0)

  when(cyclesCountReg === 0.U) {
    cyclesCountReg := CYCLES_PER_SERIAL_BIT

    when(bitsIndexReg =/= 0.U) {
      val shift = shiftReg >> 1
      shiftReg := 1.U ## shift(9, 0)
      bitsIndexReg := bitsIndexReg - 1.U
    }.otherwise {
      when(io.channel.valid) {
        // two stop bits, data, one start bit
        shiftReg := 3.U ## io.channel.bits ## 0.U
        bitsIndexReg := SEQUENCE_LENGTH.U
      }.otherwise {
        shiftReg := ELEVEN_HIGH_BITS
      }
    }

  }.otherwise {
    cyclesCountReg := cyclesCountReg - 1.U
  }
}


class Rx(frequency: Int, baudRate: Int) extends Module {
  val io = IO(new Bundle {
    val rxd = Input(UInt(1.W))
    val channel = new UartIO()
    val debugBitsReg = Output(UInt(8.W))
    val debugCntReg = Output(UInt(20.W))
  })

  val CYCLES_PER_SERIAL_BIT = ((frequency + baudRate / 2) / baudRate - 1).U
  // wait 1.5 bits after falling edge of start bit
  val CYCLES_PER_SERIAL_BIT_START = ((3 * frequency / 2 + baudRate / 2) / baudRate - 1).U

  // Sync in the asynchronous RX data, reset to 1 to not start reading after a reset
  val rxdReg = RegNext(RegNext(io.rxd, 1.U), 1.U)

  val dataBitsShiftReg = RegInit(0.U(8.W))
  val bitsCounterReg = RegInit(0.U(4.W))

  val serialBitsCounterReg = RegInit(0.U(20.W))
  val validReg = RegInit(false.B)

  io.debugBitsReg := bitsCounterReg
  io.debugCntReg := serialBitsCounterReg

  when (serialBitsCounterReg =/= 0.U) {
    serialBitsCounterReg := serialBitsCounterReg - 1.U
  }.elsewhen (bitsCounterReg =/= 0.U) {
    serialBitsCounterReg := CYCLES_PER_SERIAL_BIT
    dataBitsShiftReg := Cat(rxdReg, dataBitsShiftReg >> 1)
    bitsCounterReg := bitsCounterReg - 1.U
    // the last shifted in
    when (bitsCounterReg === 1.U) {
      validReg := true.B
    }
  }.elsewhen (rxdReg === 0.U) {
    serialBitsCounterReg := CYCLES_PER_SERIAL_BIT_START
    bitsCounterReg := 8.U
  }

  when (validReg && io.channel.ready) {
    validReg := false.B
  }

  io.channel.bits := dataBitsShiftReg
  io.channel.valid := validReg
}
