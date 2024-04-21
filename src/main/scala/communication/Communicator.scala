package communication

import chisel3._
import chisel3.util._
import communication.Encodings.{Codes, Opcodes}
import communication.chisel.lib.uart.{DeSerializingByteBuffer, SerializingByteBuffer, UartRx, UartTx}
import module_utils.{FlatVectorIntoByteSplitter, VectorIntoByteSplitter}

class Communicator(matrixByteSize: Int, frequency: Int, baudRate: Int) extends Module {

  val io = IO(new Bundle {
    val uartRxPin = Input(UInt(1.W))
    val uartTxPin = Output(UInt(1.W))

    val readEnable = Output(Bool())
    val writeEnable = Output(Bool())

    val debugState = Output(UInt(3.W))

    val dataIn = Input(Vec(10, UInt(16.W)))
    val dataOut = Output(Vec(matrixByteSize, UInt(8.W)))

    val startCalculation = Output(Bool())
    val calculationDone = Input(Bool())
  })

  io.writeEnable := false.B
  io.startCalculation := false.B
  io.readEnable := false.B

  val receivingData :: waitForExternalCalculation :: sendingData :: Nil = Enum(3)

  // vi skal kunne:
  // 1. modtage en matrix fra pc
  // 2. vente til beregningen er udført
  // 3. sende en resultatmatrix til pc

  val uartRx = Module(new UartRx(frequency, baudRate))
  val uartTx = Module(new UartTx(frequency, baudRate))

  uartRx.io.rxd := io.uartRxPin
  io.uartTxPin := uartTx.io.txd

  val bufferedInput = Module(new DeSerializingByteBuffer(matrixByteSize))
  val bufferedOutput = Module(new SerializingByteBuffer(10*2))

  // default connections
  uartTx.io.inputChannel <> bufferedOutput.io.outputChannel
  uartRx.io.outputChannel <> bufferedInput.io.inputChannel

  bufferedInput.io.outputChannel.ready := false.B
  io.dataOut := bufferedInput.io.outputChannel.bits

  bufferedOutput.io.inputChannel.valid := false.B

  val splitter = Module(new FlatVectorIntoByteSplitter(16, 10))
  splitter.io.input := io.dataIn
  bufferedOutput.io.inputChannel.bits := splitter.io.output

  // Initially, we are receiving data.
  // The state of the accelerator is "receiving" according to the FSM diagram (initial condition).
  val state = RegInit(receivingData)
  io.debugState := state

  switch(state) {
    is(receivingData) {
      bufferedInput.io.outputChannel.ready := true.B
      when(bufferedInput.io.outputChannel.valid) {
        // We are done receiving data.
        // Go to waiting state.
        io.writeEnable := true.B
        state := waitForExternalCalculation
      }
    }

    is(waitForExternalCalculation) {
      io.startCalculation := true.B // start the calculation of the layer through the layer FSM, will also increment the address
      when(io.calculationDone) { // wait until layer is done calculating
        state := sendingData // send the result back the the host
      }
    }

    is(sendingData) {
      io.readEnable := true.B
      bufferedOutput.io.inputChannel.valid := true.B
      when(bufferedOutput.io.inputChannel.ready) {
        // The output buffer is now empty.
        // We are done sending data. No more work to do here.
        // Go to receiving opcodes (idle state).
        state := receivingData
      }
    }

  }
}
