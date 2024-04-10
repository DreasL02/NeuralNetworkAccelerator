package ws_systolic_array

import chisel3._
import chisel3.util.DecoupledIO
import module_utils.{ShiftedBuffer, ShiftedOutputBuffer}
import module_utils.SmallModules.risingEdge
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
  })

  private val cyclesUntilOutputValid: Int = numberOfColumns + numberOfRows + commonDimension - 2 // number of cycles until the systolic array is done and the result is valid

  private val loadWeights = risingEdge(io.weightChannel.valid)

  private val loadInputs = risingEdge(io.inputChannel.valid)

  private val inputsBuffers = for (i <- 0 until numberOfRows) yield { // create array of buffers for inputs
    val buffer = Module(new ShiftedBuffer(w, commonDimension, i)) // shift each buffer by i to create systolic effect
    buffer
  }

  private val systolicArray = Module(new SystolicArray(w, wResult, numberOfRows, numberOfColumns, signed))

  // Connect buffer to signals
  for (i <- 0 until numberOfRows) {
    inputsBuffers(i).io.load := loadInputs
    inputsBuffers(i).io.data := io.inputChannel.bits(i)
    systolicArray.io.inputActivations(i) := inputsBuffers(i).io.output
  }

  systolicArray.io.weights := io.weightChannel.bits
  systolicArray.io.loadWeights := loadWeights

  private val outputBuffers = for (i <- 0 until numberOfColumns) yield { // create array of buffers for outputs
    val buffer = Module(new ShiftedOutputBuffer(wResult, numberOfRows, i)) // shift each buffer by i to create systolic effect
    buffer
  }


}
