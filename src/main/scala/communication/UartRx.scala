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

// Extended and refactored version of Chisel ip-contribution uart by Martin Schoeberl
// https://github.com/freechipsproject/ip-contributions/blob/master/src/main/scala/chisel/lib/uart/Uart.scala
// (visited 08-04-2024)

class UartRx(frequency: Int, baudRate: Int) extends Module {
  val io = IO(new Bundle {
    val rxd = Input(UInt(1.W))
    val cts = Output(UInt(1.W)) // Clear To Send
    val outputChannel = new DecoupledIO(UInt(8.W))
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

  when(serialBitsCounterReg =/= 0.U) {
    serialBitsCounterReg := serialBitsCounterReg - 1.U
  }.elsewhen(bitsCounterReg =/= 0.U) {
    serialBitsCounterReg := CYCLES_PER_SERIAL_BIT
    dataBitsShiftReg := Cat(rxdReg, dataBitsShiftReg >> 1)
    bitsCounterReg := bitsCounterReg - 1.U
    // the last data bit shifted in
    when(bitsCounterReg === 1.U) {
      validReg := true.B
    }
  }.elsewhen(rxdReg === 0.U) {
    serialBitsCounterReg := CYCLES_PER_SERIAL_BIT_START
    bitsCounterReg := 8.U
  }

  when(validReg && io.outputChannel.ready) {
    validReg := false.B
  }

  io.outputChannel.bits := dataBitsShiftReg
  io.outputChannel.valid := validReg

  io.cts := io.outputChannel.ready
}
