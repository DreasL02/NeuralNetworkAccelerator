import chisel3._
import chisel3.util.log2Ceil
import systolic_array.SystolicArray
import utils.Optional.optional

class BufferedSystolicArray(w: Int = 8, wStore: Int = 32, xDimension: Int = 4, yDimension: Int = 4, signed: Boolean = true, enableDebuggingIO: Boolean = true // enable debug signals for testing
                           ) extends Module {
  val io = IO(new Bundle {
    val load = Input(Bool()) // load values

    val inputs = Input(Vec(xDimension, Vec(yDimension, UInt(w.W)))) // should only be used when load is true
    val weights = Input(Vec(xDimension, Vec(yDimension, UInt(w.W)))) // should only be used when load is true

    val valid = Output(Bool()) // indicates that the systolic array should be done
    val result = Output(Vec(xDimension, Vec(yDimension, UInt(wStore.W)))) // result of layer

    val debugInputs = optional(enableDebuggingIO, Output(Vec(xDimension, UInt(w.W))))
    val debugWeights = optional(enableDebuggingIO, Output(Vec(yDimension, UInt(w.W))))
    val debugSystolicArrayResults = optional(enableDebuggingIO, Output(Vec(xDimension, Vec(yDimension, UInt(wStore.W)))))
  })


  //TODO check if correct
  val inputsBuffers = for (i <- 0 until yDimension) yield { // create array of buffers for inputs
    val buffer = Module(new ShiftedBuffer(w, yDimension, i)) // shift each buffer by i to create systolic effect
    buffer // return module
  }

  //TODO check if correct
  val weightsBuffers = for (i <- 0 until xDimension) yield { // create array of buffers for weights
    val buffer = Module(new ShiftedBuffer(w, xDimension, i)) // shift each buffer by i to create systolic effect
    buffer // return module
  }

  val systolicArray = Module(new SystolicArray(w, wStore, xDimension, yDimension, signed))

  // Connect buffers to signals
  for (i <- 0 until yDimension) {
    inputsBuffers(i).io.load := io.load
    inputsBuffers(i).io.data := io.inputs(i)
    systolicArray.io.a(i) := inputsBuffers(i).io.output
    if (enableDebuggingIO) {
      io.debugInputs.get(i) := inputsBuffers(i).io.output
    }
  }

  for (i <- 0 until xDimension) {
    weightsBuffers(i).io.load := io.load
    weightsBuffers(i).io.data := io.weights(i)
    systolicArray.io.b(i) := weightsBuffers(i).io.output
    if (enableDebuggingIO) {
      io.debugWeights.get(i) := weightsBuffers(i).io.output
    }
  }

  systolicArray.io.clear := io.load // clear systolic array when load is asserted

  // Signal that the computation is valid
  io.valid := systolicArray.io.valid

  // Connect the result of the systolic array to the output
  io.result := systolicArray.io.c

  if (enableDebuggingIO) {
    io.debugSystolicArrayResults.get := systolicArray.io.c
  }
}
