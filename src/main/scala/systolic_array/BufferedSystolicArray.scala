package systolic_array

import chisel3._
import chisel3.util.{DecoupledIO, log2Ceil}
import scala_utils.Optional.optional
import module_utils.ShiftedBuffer
import module_utils.SmallModules.{risingEdge, timer}


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
    val inputChannel = Flipped(new DecoupledIO(Vec(numberOfRows, Vec(commonDimension, UInt(w.W)))))
    val weightChannel = Flipped(new DecoupledIO(Vec(commonDimension, Vec(numberOfColumns, UInt(w.W)))))

    val resultChannel = new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W))))

    val debugInputs = optional(enableDebuggingIO, Output(Vec(numberOfRows, UInt(w.W))))
    val debugWeights = optional(enableDebuggingIO, Output(Vec(numberOfColumns, UInt(w.W))))
    val debugSystolicArrayResults = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))))
  })

  private val cyclesUntilOutputValid: Int = numberOfColumns + numberOfRows + commonDimension - 2 // number of cycles until the systolic array is done and the result is valid

  private val readyToCompute = io.inputChannel.valid && io.weightChannel.valid // ready when both inputs are valid

  private val load = risingEdge(readyToCompute) // load when readyToCompute is asserted

  private val doneWithComputation = timer(cyclesUntilOutputValid, load, readyToCompute)

  private val inputsBuffers = for (i <- 0 until numberOfRows) yield { // create array of buffers for inputs
    val buffer = Module(new ShiftedBuffer(w, commonDimension, i)) // shift each buffer by i to create systolic effect
    buffer
  }

  private val weightsBuffers = for (i <- 0 until numberOfColumns) yield { // create array of buffers for weights
    val buffer = Module(new ShiftedBuffer(w, commonDimension, i)) // shift each buffer by i to create systolic effect
    buffer
  }

  private val systolicArray = Module(new SystolicArray(w, wResult, numberOfRows, numberOfColumns, signed))


  // Connect buffers to signals
  for (i <- 0 until numberOfRows) {
    inputsBuffers(i).io.load := load
    inputsBuffers(i).io.data := io.inputChannel.bits(i)
    systolicArray.io.a(i) := inputsBuffers(i).io.output
    if (enableDebuggingIO) {
      io.debugInputs.get(i) := inputsBuffers(i).io.output
    }
  }

  private val transposedWeights = io.weightChannel.bits.transpose // transpose weights to match the systolic array's requirements

  for (i <- 0 until numberOfColumns) {
    weightsBuffers(i).io.load := load
    weightsBuffers(i).io.data := transposedWeights(i)
    systolicArray.io.b(i) := weightsBuffers(i).io.output
    if (enableDebuggingIO) {
      io.debugWeights.get(i) := weightsBuffers(i).io.output
    }
  }

  systolicArray.io.clear := load // clear systolic array when load is asserted

  // Connect the result of the systolic array to the output
  io.resultChannel.bits := systolicArray.io.c

  io.resultChannel.valid := doneWithComputation // valid when doneWithComputation is asserted
  io.inputChannel.ready := io.resultChannel.ready && io.resultChannel.valid // ready to receive new inputs when the result channel is ready and valid
  io.weightChannel.ready := io.resultChannel.ready && io.resultChannel.valid // ready to receive new weights when the result channel is ready and valid

  if (enableDebuggingIO) {
    io.debugSystolicArrayResults.get := systolicArray.io.c
  }
}

