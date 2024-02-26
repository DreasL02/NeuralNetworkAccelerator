package systolic_array

import chisel3._
import chisel3.util.log2Ceil
import scala_utils.Optional.optional
import module_utils.ShiftedBuffer

class BufferedSystolicArray(
                             w: Int = 8,
                             wResult: Int = 32,
                             numberOfRows: Int = 4, // number of rows in the result matrix / number of rows in the first matrix
                             numberOfColumns: Int = 4, // number of columns in the result matrix / number of columns in the second matrix
                             commonDimension: Int = 4, // number of columns in the first matrix and number of rows in the second matrix
                             signed: Boolean = true,
                             enableDebuggingIO: Boolean = true
                           ) extends Module {
  val io = IO(new Bundle {
    val load = Input(Bool()) // load values

    val inputs = Input(Vec(numberOfRows, Vec(commonDimension, UInt(w.W)))) // should only be used when load is true
    val weights = Input(Vec(numberOfColumns, Vec(commonDimension, UInt(w.W)))) // should only be used when load is true

    val valid = Output(Bool()) // indicates that the systolic array should be done
    val result = Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))) // result of layer

    val debugInputs = optional(enableDebuggingIO, Output(Vec(numberOfRows, UInt(w.W))))
    val debugWeights = optional(enableDebuggingIO, Output(Vec(numberOfColumns, UInt(w.W))))
    val debugSystolicArrayResults = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))))
  })

  val CYCLES_UNTIL_VALID: Int = numberOfColumns + numberOfRows + commonDimension - 2 // number of cycles until the systolic array is done and the result is valid

  def timer(max: Int, reset: Bool): Bool = { // timer that counts up to max and then resets, can also be reset manually by asserting reset
    val x = RegInit(0.U(log2Ceil(max + 1).W))
    val done = x === max.U // done when x reaches max
    x := Mux(done || reset, 0.U, x + 1.U) // reset when done or reset is asserted, otherwise increment
    done
  }

  io.valid := timer(CYCLES_UNTIL_VALID, io.load) // valid when timer is done

  val inputsBuffers = for (i <- 0 until numberOfRows) yield { // create array of buffers for inputs
    val buffer = Module(new ShiftedBuffer(w, commonDimension, i)) // shift each buffer by i to create systolic effect
    buffer // return module
  }

  val weightsBuffers = for (i <- 0 until numberOfColumns) yield { // create array of buffers for weights
    val buffer = Module(new ShiftedBuffer(w, commonDimension, i)) // shift each buffer by i to create systolic effect
    buffer // return module
  }

  val systolicArray = Module(new SystolicArray(w, wResult, numberOfRows, numberOfColumns, signed))

  // Connect buffers to signals
  for (i <- 0 until numberOfRows) {
    inputsBuffers(i).io.load := io.load
    inputsBuffers(i).io.data := io.inputs(i)
    systolicArray.io.a(i) := inputsBuffers(i).io.output
    if (enableDebuggingIO) {
      io.debugInputs.get(i) := inputsBuffers(i).io.output
    }
  }

  for (i <- 0 until numberOfColumns) {
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
