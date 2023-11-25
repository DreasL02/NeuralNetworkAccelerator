package communication

import chisel3._
import chisel3.util._
import communication.Encodings.{Codes, Opcodes}
import communication.chisel.lib.uart.{BufferedUartRxForTestingOnly, BufferedUartTxForTestingOnly}

class Communicator(matrixByteSize: Int, frequency: Int, baudRate: Int) extends Module {

  val io = IO(new Bundle {
    val uartRxPin = Input(UInt(1.W))
    val uartTxPin = Output(UInt(1.W))

    val incrementAddress = Output(Bool())
    //val readEnable = Output(Bool())
    val writeEnable = Output(Bool())
    val ready = Output(Bool())

    //val dataIn = Input(Vec(matrixByteSize, UInt(8.W)))
    val dataOut = Output(Vec(matrixByteSize, UInt(8.W)))
  })

  io.incrementAddress := false.B
  io.writeEnable := false.B
  io.ready := false.B

  val receivingOpcodes :: respondingWithOKSignal :: incrementingAddress :: receivingData :: sendingData :: Nil = Enum(5)

  // TODO: All these modules should ideally utilize a shared a single UartRx and a single UartTx.
  val bufferedOpcodeInput = Module(new BufferedUartRxForTestingOnly(frequency, baudRate, 1))
  val bufferedDataInput = Module(new BufferedUartRxForTestingOnly(frequency, baudRate, matrixByteSize))
  val bufferedDataOutput = Module(new BufferedUartTxForTestingOnly(frequency, baudRate, matrixByteSize))

  bufferedOpcodeInput.io.rxd := io.uartRxPin
  bufferedDataInput.io.rxd := 1.U(1.W)

  val countDown = RegInit(35.U)

  io.uartTxPin := bufferedDataOutput.io.txd

  bufferedOpcodeInput.io.outputChannel.ready := false.B
  bufferedDataInput.io.outputChannel.ready := false.B
  bufferedDataOutput.io.inputChannel.valid := false.B

  bufferedDataOutput.io.inputChannel.bits := bufferedDataInput.io.outputChannel.bits // TODO: This is a hack.

  io.dataOut := bufferedDataInput.io.outputChannel.bits

  // Initially, we are receiving opcodes.
  // The state of the accelerator is "receiving" according to the FSM diagram (initial condition).
  val state = RegInit(receivingOpcodes)

  val decoder = Module(new Decoder)

  decoder.io.opcode := bufferedOpcodeInput.io.outputChannel.bits(0)

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

    is(respondingWithOKSignal) {

    }

    is(incrementingAddress) {
      io.incrementAddress := true.B
      state := receivingOpcodes //TODO : This should be respondingWithOKSignal.
    }

    is(receivingData) {

      bufferedDataInput.io.rxd := io.uartRxPin

      when (countDown =/= 0.U) {
        bufferedDataInput.io.rxd := 1.U(1.W)
        countDown := countDown - 1.U
      }

      bufferedDataInput.io.outputChannel.ready := true.B
      when(bufferedDataInput.io.outputChannel.valid) {
        // We are done receiving data.
        // Go to sending OK signal.
        // TODO: Where does the data go?
        // TODO: Map the data to memory.
        io.writeEnable := true.B
        state := receivingOpcodes // TODO: This should be respondingWithOKSignal.
      }
    }

    is(sendingData) {
      when(bufferedDataOutput.io.inputChannel.ready) {
        // The output buffer is now empty.
        // We are done sending data. No more work to do here.
        // Go to sending OK signal.
        state := respondingWithOKSignal
      }
    }
  }
}
