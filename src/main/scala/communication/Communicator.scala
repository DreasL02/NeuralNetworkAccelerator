package communication

import chisel3._
import chisel3.util._
import communication.Encodings.{Codes, Opcodes}
import communication.chisel.lib.uart.{BufferedUartRx, BufferedUartTx}

class Communicator extends Module {
  val io = IO(new Bundle {
    val uartRxPin = Input(Bool())
    val uartTxPin = Output(Bool())

    val valid = Output(Bool())
    val opcode = Output(UInt(Opcodes.opcodeWidth.W))
  })

  val idle :: receivingOpcodes :: actOnUpcode :: receivingAddress :: receivingData :: sendingData :: Nil = Enum(4)

  val frequency = 100
  val baudRate = 1

  val MATRIX_DIMENSION = 3
  val MATRIX_OPERAND_BYTE_SIZE = 1
  val MATRIX_BYTE_SIZE = MATRIX_OPERAND_BYTE_SIZE * MATRIX_DIMENSION * MATRIX_DIMENSION

  // TODO: All these modules should ideally utilize a shared a single UartRx and a single UartTx.
  val bufferedOpcodeInput = Module(new BufferedUartRx(frequency, baudRate, 1))
  val bufferedDataInput = Module(new BufferedUartRx(frequency, baudRate, MATRIX_BYTE_SIZE))
  val bufferedDataOutput = Module(new BufferedUartTx(frequency, baudRate, MATRIX_BYTE_SIZE))

  // Initially, we are receiving opcodes.
  // The state of the accelerator is "receiving" according to the FSM diagram (initial condition).
  val state = RegInit(receivingOpcodes)

  val decoder = Module(new Decoder)

  decoder.io.opcode := bufferedOpcodeInput.io.channel.bits

  switch(state) {

    is(idle) {
      // Does this state make sense? Can we just wait for opcodes as an idle state instead?
      when(bufferedOpcodeInput.io.channel.valid) {
        state := receivingOpcodes
      }
    }

    is(receivingOpcodes) {
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
            state := receivingAddress
            // TODO: Are addresses the same size as opcodes?
          }
        }
      }
    }

    is(receivingAddress) {

      when(bufferedOpcodeInput.io.channel.valid) {
        // The address input buffer is now full.
        // We are done receiving the address.
        // Go back to receiving opcodes.
        // TODO: Where does the address go?
        state := receivingOpcodes
      }

    }

    is(receivingData) {
      when(bufferedDataInput.io.channel.valid) {
        // We are done receiving data.
        // Go back to receiving opcodes.
        // TODO: Where does the data go?
        // TODO: Map the data to memory.
        // TODO: Must me "unrolled" from "bits" vector og 8 bit registers.
        state := receivingOpcodes
      }
    }

    is(sendingData) {
      when (bufferedDataOutput.io.channel.ready) {
        // The output buffer is now empty.
        // We are done sending data. No more work to do here.
        // Go back to receiving opcodes (or idle?).
        state := receivingOpcodes
      }
    }
  }
}
