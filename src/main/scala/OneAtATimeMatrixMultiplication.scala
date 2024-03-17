import chisel3._
import chisel3.util.{DecoupledIO, log2Ceil}
import module_utils.SmallModules.{mult, risingEdge, timer}
import scala_utils.Optional.optional

class OneAtATimeMatrixMultiplication(
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

    val debugCounters = optional(enableDebuggingIO, Output(Vec(3, UInt(log2Ceil(math.max(numberOfRows, math.max(numberOfColumns, commonDimension))).W))))
  })

  // Number of cycles required to compute the result matrix: numberOfRows * numberOfColumns * commonDimension
  val cyclesUntilOutputValid = numberOfRows * numberOfColumns * commonDimension

  val readyToCompute = io.inputChannel.valid && io.weightChannel.valid // ready when both inputs are valid

  val load = risingEdge(readyToCompute) // load when readyToCompute is asserted

  val doneWithComputation = timer(cyclesUntilOutputValid, load, readyToCompute)

  val inputsReg = Reg(Vec(numberOfRows, Vec(commonDimension, UInt(w.W))))
  val weightsReg = Reg(Vec(commonDimension, Vec(numberOfColumns, UInt(w.W))))
  val resultsReg = Reg(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W))))

  when(load) {
    inputsReg := io.inputChannel.bits
    weightsReg := io.weightChannel.bits
  }

  val rowCounter = RegInit(0.U(log2Ceil(numberOfRows).W))
  val columnCounter = RegInit(0.U(log2Ceil(numberOfColumns).W))
  val commonCounter = RegInit(0.U(log2Ceil(commonDimension).W))

  when(load) {
    commonCounter := 0.U
    rowCounter := 0.U
    columnCounter := 0.U
  }

  when(commonCounter === (commonDimension - 1).U) {
    commonCounter := 0.U
    when(columnCounter === (numberOfColumns - 1).U) {
      columnCounter := 0.U
      when(rowCounter === (numberOfRows - 1).U) {
        rowCounter := 0.U
      }.otherwise {
        rowCounter := rowCounter + 1.U
      }
    }.otherwise {
      columnCounter := columnCounter + 1.U
    }
  }.otherwise {
    commonCounter := commonCounter + 1.U
  }

  val cycleInput = inputsReg(rowCounter)(commonCounter)
  val cycleWeight = weightsReg(commonCounter)(columnCounter)

  resultsReg(rowCounter)(columnCounter) := mult(cycleInput, cycleWeight, w, wResult, signed) + resultsReg(rowCounter)(columnCounter)

  io.resultChannel.bits := resultsReg

  io.resultChannel.valid := doneWithComputation // ready when doneWithComputation is asserted
  io.inputChannel.ready := io.resultChannel.ready && io.resultChannel.valid // ready to receive new inputs when the result channel is ready and valid
  io.weightChannel.ready := io.resultChannel.ready && io.resultChannel.valid // ready to receive new weights when the result channel is ready and valid

  if (enableDebuggingIO) {
    io.debugCounters.get(0) := rowCounter
    io.debugCounters.get(1) := columnCounter
    io.debugCounters.get(2) := commonCounter
  }
}
