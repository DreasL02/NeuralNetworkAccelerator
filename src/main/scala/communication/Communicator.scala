package communication

import chisel3._
import chisel3.util._
import communication.Encodings.{Codes, Opcodes}
import communication.chisel.lib.uart.{UartRx, UartTx, SerializingByteBuffer, DeSerializingByteBuffer}

class Communicator(matrixByteSize: Int, frequency: Int, baudRate: Int) extends Module {

  val io = IO(new Bundle {
    val uartRxPin = Input(UInt(1.W))
    val uartTxPin = Output(UInt(1.W))

    val incrementAddress = Output(Bool())
    val readEnable = Output(Bool())
    val writeEnable = Output(Bool())
    val ready = Output(Bool())

    val dataIn = Input(Vec(matrixByteSize, UInt(8.W)))
    val dataOut = Output(Vec(matrixByteSize, UInt(8.W)))

    val startCalculation = Output(Bool())
    val calculationDone = Input(Bool())
  })

  io.incrementAddress := false.B
  io.writeEnable := false.B
  io.ready := false.B
  io.startCalculation := false.B
  io.readEnable := false.B
  val receivingOpcodes :: respondingWithOKSignal :: incrementingAddress :: receivingData :: sendingData :: waitForExternalCalculation :: Nil = Enum(6)

  val uartRx = Module(new UartRx(frequency, baudRate))
  val uartTx = Module(new UartTx(frequency, baudRate))

  uartRx.io.rxd := io.uartRxPin
  io.uartTxPin := uartTx.io.txd

  val bufferedOpcodeInput = Module(new DeSerializingByteBuffer(1))
  val bufferedReadyOutput = Module(new SerializingByteBuffer(1))

  val bufferedDataInput = Module(new DeSerializingByteBuffer(matrixByteSize))
  val bufferedDataOutput = Module(new SerializingByteBuffer(matrixByteSize))
  val decoder = Module(new Decoder)

  // default connections
  uartTx.io.inputChannel <> bufferedDataOutput.io.outputChannel
  uartRx.io.outputChannel <> bufferedOpcodeInput.io.inputChannel

  bufferedOpcodeInput.io.outputChannel.ready := false.B
  decoder.io.opcode := bufferedOpcodeInput.io.outputChannel.bits(0)

  bufferedReadyOutput.io.inputChannel.bits := VecInit(Seq(Opcodes.okResponse))
  bufferedReadyOutput.io.inputChannel.valid := false.B
  bufferedReadyOutput.io.outputChannel.ready := false.B

  bufferedDataInput.io.inputChannel.bits := 0.U
  bufferedDataInput.io.inputChannel.valid := false.B
  bufferedDataInput.io.outputChannel.ready := false.B
  io.dataOut := bufferedDataInput.io.outputChannel.bits

  bufferedDataOutput.io.inputChannel.bits := io.dataIn
  bufferedDataOutput.io.inputChannel.valid := false.B


  // Initially, we are receiving opcodes.
  // The state of the accelerator is "receiving" according to the FSM diagram (initial condition).
  val state = RegInit(receivingOpcodes)


  switch(state) {
    is(receivingOpcodes) {
      bufferedOpcodeInput.io.outputChannel.ready := true.B
      io.ready := true.B
      when(bufferedOpcodeInput.io.outputChannel.valid) {
        // We have loaded the opcode into the buffer and decoded it.
        switch(decoder.io.decodingCode) {
          is(Codes.nextInputs) {
            state := receivingData
          }
          is(Codes.nextTransmitting) {
            state := sendingData
          }
          is(Codes.nextCalculating) {
            state := waitForExternalCalculation
          }
          is(Codes.nextAddress) {
            state := incrementingAddress
          }
        }
      }
    }

    is(respondingWithOKSignal) {
      bufferedReadyOutput.io.inputChannel.valid := true.B
      uartTx.io.inputChannel <> bufferedReadyOutput.io.outputChannel
      when(bufferedReadyOutput.io.inputChannel.ready) {
        // We are done sending the OK signal.
        // Go to receiving opcodes (idle state).
        state := receivingOpcodes
      }
    }

    is(incrementingAddress) {
      io.incrementAddress := true.B
      state := respondingWithOKSignal
    }

    is(receivingData) {
      uartRx.io.outputChannel <> bufferedDataInput.io.inputChannel
      bufferedDataInput.io.outputChannel.ready := true.B
      when(bufferedDataInput.io.outputChannel.valid) {
        // We are done receiving data.
        // Go to sending OK signal.
        io.writeEnable := true.B
        state := respondingWithOKSignal
      }
    }

    is(sendingData) {
      uartTx.io.inputChannel <> bufferedDataOutput.io.outputChannel
      bufferedDataOutput.io.inputChannel.valid := true.B
      io.readEnable := true.B
      when(bufferedDataOutput.io.inputChannel.ready) {
        // The output buffer is now empty.
        // We are done sending data. No more work to do here.
        // Go to sending OK signal.
        state := respondingWithOKSignal
      }
    }

    is(waitForExternalCalculation) {
      io.startCalculation := true.B // start the calculation of the layer through the layer FSM, will also increment the address

      when(io.calculationDone) { // wait until layer is done calculating
        state := respondingWithOKSignal
      }
    }
  }
}
