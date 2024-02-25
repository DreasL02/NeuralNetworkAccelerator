package systolic_array

import chisel3._
import chisel3.util.log2Ceil
import scala_utils.Optional.optional
import module_utils.ShiftedBuffer

class BufferedSystolicArray(
                             w: Int = 8,
                             wBig: Int = 32,
                             xDimension: Int = 4,
                             yDimension: Int = 4,
                             commonDimension: Int = 4,
                             signed: Boolean = true,
                             enableDebuggingIO: Boolean = true
                           ) extends Module {
  val io = IO(new Bundle {
    val load = Input(Bool()) // load values

    val inputs = Input(Vec(yDimension, Vec(commonDimension, UInt(w.W)))) // should only be used when load is true
    val weights = Input(Vec(xDimension, Vec(commonDimension, UInt(w.W)))) // should only be used when load is true

    val valid = Output(Bool()) // indicates that the systolic array should be done
    val result = Output(Vec(xDimension, Vec(yDimension, UInt(wBig.W)))) // result of layer

    val debugInputs = optional(enableDebuggingIO, Output(Vec(xDimension, UInt(w.W))))
    val debugWeights = optional(enableDebuggingIO, Output(Vec(yDimension, UInt(w.W))))
    val debugSystolicArrayResults = optional(enableDebuggingIO, Output(Vec(xDimension, Vec(yDimension, UInt(wBig.W)))))
  })

  val CYCLES_UNTIL_VALID: Int = xDimension * yDimension - 1 // number of cycles until the systolic array is done and the result is valid

  def timer(max: Int, reset: Bool): Bool = { // timer that counts up to max and then resets, can also be reset manually by asserting reset
    val x = RegInit(0.U(log2Ceil(max + 1).W))
    val done = x === max.U // done when x reaches max
    x := Mux(done || reset, 0.U, x + 1.U) // reset when done or reset is asserted, otherwise increment
    done
  }

  io.valid := timer(CYCLES_UNTIL_VALID, io.load) // valid when timer is done

  //TODO check if correct
  val inputsBuffers = for (i <- 0 until yDimension) yield { // create array of buffers for inputs
    val buffer = Module(new ShiftedBuffer(w, commonDimension, i)) // shift each buffer by i to create systolic effect
    buffer // return module
  }

  //TODO check if correct
  val weightsBuffers = for (i <- 0 until xDimension) yield { // create array of buffers for weights
    val buffer = Module(new ShiftedBuffer(w, commonDimension, i)) // shift each buffer by i to create systolic effect
    buffer // return module
  }

  val systolicArray = Module(new SystolicArray(w, wBig, xDimension, yDimension, signed))

  // Connect buffers to signals
  for (i <- 0 until yDimension) { // for each buffer
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

  // Connect the result of the systolic array to the output
  io.result := systolicArray.io.c

  if (enableDebuggingIO) {
    io.debugSystolicArrayResults.get := systolicArray.io.c
  }
}
