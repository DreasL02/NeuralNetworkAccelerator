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

    val states = Output(Vec(6, Bool()))

    val dataIn = Input(Vec(matrixByteSize, UInt(8.W)))
    val dataOut = Output(Vec(matrixByteSize, UInt(8.W)))

    val startCalculation = Output(Bool())
    val calculationDone = Input(Bool())
  })

  io.incrementAddress := false.B
  io.writeEnable := false.B
  io.states := VecInit(Seq.fill(6)(false.B))
  io.startCalculation := false.B
  io.readEnable := false.B
  val receivingOpcodes :: respondingWithOKSignal :: incrementingAddress :: receivingData :: sendingData :: waitForExternalCalculation :: Nil = Enum(6)

  val uartRx = Module(new UartRx(frequency, baudRate))
  val uartTx = Module(new UartTx(frequency, baudRate))

  uartRx.io.rxd := io.uartRxPin
  io.uartTxPin := uartTx.io.txd

  val bufferedInput = Module(new DeSerializingByteBuffer(matrixByteSize))
  val bufferedOutput = Module(new SerializingByteBuffer(matrixByteSize))

  val decoder = Module(new Decoder)

  // default connections
  uartTx.io.inputChannel <> bufferedOutput.io.outputChannel
  uartRx.io.outputChannel <> bufferedInput.io.inputChannel

  bufferedInput.io.outputChannel.ready := false.B
  io.dataOut := bufferedInput.io.outputChannel.bits
  decoder.io.opcode := bufferedInput.io.outputChannel.bits(0)

  bufferedOutput.io.inputChannel.valid := false.B
  bufferedOutput.io.inputChannel.bits := io.dataIn

  // Initially, we are receiving opcodes.
  // The state of the accelerator is "receiving" according to the FSM diagram (initial condition).
  val state = RegInit(receivingOpcodes)


  switch(state) {
    is(receivingOpcodes) {
      io.states(0) := true.B
      bufferedInput.io.outputChannel.ready := true.B
      when(bufferedInput.io.outputChannel.valid) {
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
      io.states(1) := true.B
      bufferedOutput.io.inputChannel.bits := VecInit(Seq.fill(matrixByteSize)(Opcodes.okResponse))
      bufferedOutput.io.inputChannel.valid := true.B
      when(bufferedOutput.io.inputChannel.ready) {
        // We are done sending the OK signal.
        // Go to receiving opcodes (idle state).
        state := receivingOpcodes
      }
    }

    is(incrementingAddress) {
      io.states(2) := true.B
      io.incrementAddress := true.B
      state := respondingWithOKSignal // send affirmative signal
    }

    is(receivingData) {
      io.states(3) := true.B
      bufferedInput.io.outputChannel.ready := true.B
      when(bufferedInput.io.outputChannel.valid) {
        // We are done receiving data.
        // Go to sending OK signal.
        io.writeEnable := true.B
        state := respondingWithOKSignal
      }
    }

    is(sendingData) {
      io.states(4) := true.B
      io.readEnable := true.B
      bufferedOutput.io.inputChannel.valid := true.B
      when(bufferedOutput.io.inputChannel.ready) {
        // The output buffer is now empty.
        // We are done sending data. No more work to do here.
        // Go to receiving opcodes (idle state).
        state := receivingOpcodes
      }
    }

    is(waitForExternalCalculation) {
      io.states(5) := true.B
      io.startCalculation := true.B // start the calculation of the layer through the layer FSM, will also increment the address
      when(io.calculationDone) { // wait until layer is done calculating
        state := respondingWithOKSignal // send affirmative signal
      }
    }
  }
}
