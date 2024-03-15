import chisel3._
import chisel3.util.{DecoupledIO, log2Ceil}
import scala_utils.SmallModules._

// Inspired by https://github.com/pConst/basic_verilog/blob/master/adder_tree.sv

class AdderTree(
                 w: Int = 8,
                 numberOfInputs: Int = 4, // number of columns in the first matrix and number of rows in the second matrix

               ) extends Module {

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(numberOfInputs, UInt(w.W))))
    val resultChannel = new DecoupledIO(UInt(w.W))
  })

  val numberOfStages = log2Ceil(numberOfInputs) + 1
  val numberOfInputsInt = math.pow(2, numberOfStages - 1).toInt
  //println("numberOfStages: " + numberOfStages)
  //println("numberOfInputsInt: " + numberOfInputsInt)

  val data = Wire(Vec(numberOfStages, Vec(numberOfInputsInt, UInt(w.W))))

  for (stage <- 0 until numberOfStages) {
    val numberOfOutputs = numberOfInputsInt >> stage
    //println("numberOfOutputs: " + numberOfOutputs)

    if (stage == 0) {
      for (adder <- 0 until numberOfInputsInt) {
        if (adder < numberOfInputs) {
          data(stage)(adder) := io.inputChannel.bits(adder)
          //println("Connected (stage, adder): (" + stage + ", " + adder + ")" + " to input channel " + adder)
        } else {
          data(stage)(adder) := 0.U
          //println("Connected (stage, adder): (" + stage + ", " + adder + ")" + " to 0")
        }
      }
    } else {
      for (adder <- 0 until numberOfInputsInt) {
        if (adder < numberOfOutputs) {
          data(stage)(adder) := RegNext(data(stage - 1)(adder * 2) + data(stage - 1)(adder * 2 + 1))
          //println("Connected (stage, adder): (" + stage + ", " + adder + ")" + " to previous stage " + (adder * 2) + " and " + (adder * 2 + 1))
        } else {
          data(stage)(adder) := 0.U
          //println("Connected (stage, adder): (" + stage + ", " + adder + ")" + " to 0")
        }
      }
    }
  }

  io.resultChannel.bits := data(numberOfStages - 1)(0)

  val cyclesUntilOutputValid: Int = numberOfStages - 2 // number of cycles until the adder tree is done and the result is valid

  val readyToCompute = io.inputChannel.valid

  val clear = risingEdge(readyToCompute) // load when readyToCompute is asserted

  val doneWithComputation = timer(cyclesUntilOutputValid, clear, readyToCompute)

  io.resultChannel.valid := doneWithComputation // ready when doneWithComputation is asserted
  io.inputChannel.ready := io.resultChannel.ready && io.resultChannel.valid // ready to receive new inputs when the result channel is ready and valid
}
