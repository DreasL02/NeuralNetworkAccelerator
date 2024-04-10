package ws_systolic_array

import chisel3._

class SystolicArray(
                     w: Int = 8, // width of the inputs
                     wResult: Int = 32, // width of the result
                     numberOfRows: Int = 4, // number of rows in the result matrix (number of PEs in the vertical direction)
                     numberOfColumns: Int = 4, // number of columns in the result matrix (number of PEs in the horizontal direction)
                     signed: Boolean = true // to determine if signed or unsigned multiplication should be used
                   ) extends Module {

  val io = IO(new Bundle {
    val inputActivations = Input(Vec(numberOfRows, UInt(w.W))) // values shifted in from the left, equal to the number of rows in the systolic array
    val weights = Input(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))
    val partialSums = Output(Vec(numberOfColumns, UInt(wResult.W)))

    val loadWeights = Input(Bool()) // loads weights into the PEs
    val clear = Input(Bool()) // clears all registers in the PEs
  })

  private val processingElements = VecInit.fill(numberOfRows, numberOfColumns)(Module(new ProcessingElement(w, wResult, signed)).io)

  for (row <- 0 until numberOfRows) {
    for (column <- 0 until numberOfColumns) {
      //Vertical inputs
      if (column == 0) {
        //Take from buffer
        processingElements(row)(0).inputActivationIn := io.inputActivations(row)
      } else {
        //Take from previous PE
        processingElements(row)(column).inputActivationIn := processingElements(row)(column - 1).inputActivationOut
      }

      // Horizontal outputs
      if (row == numberOfRows - 1) {
        //Take from bottom row
        io.partialSums(column) := processingElements(row)(column).partialSumOut
      } else if (row == 0) {
        // First row has no partial sums
        processingElements(0)(column).partialSumIn := 0.U
      } else {
        //Take from previous PE
        processingElements(row)(column).partialSumIn := processingElements(row - 1)(column).partialSumOut
      }

      processingElements(row)(column).weightPreload := io.weights(row)(column)
      processingElements(row)(column).loadWeight := io.loadWeights
      processingElements(row)(column).clear := io.clear
    }
  }


}
