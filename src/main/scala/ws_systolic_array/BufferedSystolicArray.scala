package ws_systolic_array

import chisel3._
import chisel3.util.DecoupledIO
import module_utils.{ShiftedBuffer, ShiftedOutputBuffer}
import module_utils.SmallModules.{risingEdge, timer}
import scala_utils.Optional.optional

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

    val debugInputs = optional(enableDebuggingIO, Output(Vec(commonDimension, UInt(w.W))))
    val debugWeights = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W)))))
    val debugPartialResults = optional(enableDebuggingIO, Output(Vec(numberOfColumns, UInt(wResult.W))))
    val debugResults = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))))
    val debugPartialSums = optional(enableDebuggingIO, Output(Vec(commonDimension, Vec(numberOfColumns, UInt(wResult.W)))))

  })

  private val cyclesUntilOutputValid: Int = numberOfColumns + numberOfRows + commonDimension - 2 // number of cycles until the systolic array is done and the result is valid

  private val loadWeights = risingEdge(io.weightChannel.valid)

  private val loadInputs = risingEdge(io.inputChannel.valid)

  private val doneWithComputation = timer(cyclesUntilOutputValid, loadWeights || loadInputs, io.inputChannel.valid && io.weightChannel.valid)

  private val systolicArray = Module(new SystolicArray(w, wResult, commonDimension, numberOfColumns, signed, enableDebuggingIO))

  private val inputsBuffers = for (i <- 0 until commonDimension) yield {
    val buffer = Module(new ShiftedBuffer(w, numberOfRows, i)) // shift each buffer by i to create systolic effect
    buffer
  }

  for (i <- 0 until commonDimension) {
    inputsBuffers(i).io.load := loadInputs
    inputsBuffers(i).io.data := io.inputChannel.bits(i)
    systolicArray.io.inputActivations(i) := inputsBuffers(i).io.output
    if (enableDebuggingIO) {
      io.debugInputs.get(i) := inputsBuffers(i).io.output
    }
  }

  systolicArray.io.weights := io.weightChannel.bits
  systolicArray.io.loadWeights := loadWeights
  systolicArray.io.clear := doneWithComputation

  private val outputBuffers = for (i <- 0 until numberOfColumns) yield {
    val buffer = Module(new ShiftedOutputBuffer(wResult, numberOfRows, i))
    buffer
  }

  for (i <- 0 until numberOfColumns) {
    outputBuffers(i).io.data := systolicArray.io.partialSums(i)
    io.resultChannel.bits(i) := outputBuffers(i).io.output
    if (enableDebuggingIO) {
      io.debugPartialResults.get(i) := systolicArray.io.partialSums(i)
    }
  }


  io.resultChannel.valid := doneWithComputation // valid when doneWithComputation is asserted
  io.inputChannel.ready := io.resultChannel.ready && io.resultChannel.valid // ready to receive new inputs when the result channel is ready and valid
  io.weightChannel.ready := io.resultChannel.ready && io.resultChannel.valid // ready to receive new weights when the result channel is ready and valid

  if (enableDebuggingIO) {
    io.debugWeights.get := io.weightChannel.bits
    io.debugResults.get := io.resultChannel.bits
    io.debugPartialSums.get := systolicArray.io.debugPartialSums.get
  }
}
