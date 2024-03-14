import chisel3._
import chisel3.util.{DecoupledIO, log2Ceil}
import scala_utils.SmallModules._

class AdderTree(
                 w: Int = 8,
                 numberOfValues: Int = 4, // number of columns in the first matrix and number of rows in the second matrix
                 enableDebuggingIO: Boolean = true
               ) extends Module {

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(numberOfValues, UInt(w.W))))
    val resultChannel = new DecoupledIO(UInt(w.W))
  })

  // A adder tree is a tree of adders that sums all the values in the input channel
  // The tree is built by adding pairs of values until there is only one value left
  // The result is then valid and ready to be read
  // Each stage of the tree is a register that holds the sum of the two values from the previous stage

  val numberOfLevels = log2Ceil(numberOfValues)

  for (level <- 0 until numberOfLevels) {
    val levelOutputs = Wire(Vec(numberOfValues / (1 << (level + 1)), UInt(w.W)))
    if (level == 0) {
      for (i <- 0 until levelOutputs.length by 2) {
        levelOutputs(i / 2) := io.inputChannel.bits(i) + io.inputChannel.bits(i + 1)
      }
    } else {
      for (i <- 0 until levelOutputs.length by 2) {
        levelOutputs(i / 2) := RegNext(levelOutputs(i) + levelOutputs(i + 1))
      }
    }

    if (level == numberOfLevels - 1) {
      io.resultChannel.bits := levelOutputs(0)
    }
  }

  val cyclesUntilOutputValid: Int = numberOfLevels // number of cycles until the adder tree is done and the result is valid

  val readyToCompute = io.inputChannel.valid

  val clear = risingEdge(readyToCompute) // load when readyToCompute is asserted

  val doneWithComputation = timer(cyclesUntilOutputValid, clear, readyToCompute)

  io.resultChannel.valid := doneWithComputation // ready when doneWithComputation is asserted
  io.inputChannel.ready := io.resultChannel.ready && io.resultChannel.valid // ready to receive new inputs when the result channel is ready and valid
}
