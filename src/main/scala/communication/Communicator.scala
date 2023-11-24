package communication

import chisel3._
import chisel3.util._
import communication.Encodings.{Codes, Opcodes}
import communication.chisel.lib.uart.{BufferedUartRx, BufferedUartTx}

class Communicator(matrixByteSize: Int, frequency: Int, baudRate: Int) extends Module {
  val io = IO(new Bundle {
    val uartRxPin = Input(UInt(1.W))
    val uartTxPin = Output(UInt(1.W))

    val incrementAddress = Output(Bool())
  })

  io.incrementAddress := false.B

  val receivingOpcodes :: respondingWithOKSignal :: incrementingAddress :: receivingData :: sendingData :: Nil = Enum(5)

  // TODO: All these modules should ideally utilize a shared a single UartRx and a single UartTx.
  val bufferedOpcodeInput = Module(new BufferedUartRx(frequency, baudRate, 1))
  val bufferedDataInput = Module(new BufferedUartRx(frequency, baudRate, matrixByteSize))
  val bufferedDataOutput = Module(new BufferedUartTx(frequency, baudRate, matrixByteSize))

  bufferedOpcodeInput.io.rxd := io.uartRxPin
  bufferedDataInput.io.rxd := io.uartRxPin
  io.uartTxPin := bufferedDataOutput.io.txd

  bufferedOpcodeInput.io.channel.ready := false.B
  bufferedDataInput.io.channel.ready := false.B
  bufferedDataOutput.io.channel.valid := false.B

  bufferedDataOutput.io.channel.bits := bufferedDataInput.io.channel.bits // TODO: This is a hack.

  // Initially, we are receiving opcodes.
  // The state of the accelerator is "receiving" according to the FSM diagram (initial condition).
  val state = RegInit(receivingOpcodes)

  val decoder = Module(new Decoder)

  decoder.io.opcode := bufferedOpcodeInput.io.channel.bits(0)

  switch(state) {

    is(receivingOpcodes) {
      bufferedOpcodeInput.io.channel.ready := true.B

      when (bufferedOpcodeInput.io.channel.valid) {
        // We have loaded the opcode into the buffer and decoded it.
        switch(decoder.io.decodingCode) {
          is(Codes.nextInputs) {
            state := receivingData
          }
          is(Codes.nextTransmitting) {
            state := sendingData
          }
          is(Codes.nextReading) {
            state := receivingData
          }
          is(Codes.nextAddress) {
            state := incrementingAddress
            // TODO: Are addresses the same size as opcodes?
          }
        }
      }
    }

    is (respondingWithOKSignal) {

    }

    is(incrementingAddress) {
      io.incrementAddress := true.B
      state := receivingOpcodes
    }

    is(receivingData) {
      when(bufferedDataInput.io.channel.valid) {
        // We are done receiving data.
        // Go to sending OK signal.
        // TODO: Where does the data go?
        // TODO: Map the data to memory.
        // TODO: Must me "unrolled" from "bits" vector og 8 bit registers.
        state := respondingWithOKSignal
      }
    }

    is(sendingData) {
      when (bufferedDataOutput.io.channel.ready) {
        // The output buffer is now empty.
        // We are done sending data. No more work to do here.
        // Go to sending OK signal.
        state := respondingWithOKSignal
      }
    }
  }
}
