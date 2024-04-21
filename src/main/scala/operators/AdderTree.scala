package operators

import chisel3._
import chisel3.util.{DecoupledIO, log2Ceil}
import module_utils.CalculationDelayInterfaceFSM
import module_utils.SmallModules._

// General design is inspired by the approach presented at
// https://github.com/pConst/basic_verilog/blob/master/adder_tree.sv (visited on 08-04-2024)
// by Konstantin Pavlov.

class AdderTree(
                 w: Int,
                 numberOfInputs: Int, // number of columns in the first matrix and number of rows in the second matrix
                 toPrint: Boolean = false
               ) extends Module {

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(numberOfInputs, UInt(w.W))))
    val outputChannel = new DecoupledIO(UInt(w.W))
  })

  private val numberOfStages = log2Ceil(numberOfInputs) + 1
  private val numberOfInputsInt = math.pow(2, numberOfStages - 1).toInt

  if (toPrint) {
    println("numberOfStages: " + numberOfStages)
    println("numberOfInputsInt: " + numberOfInputsInt)
  }

  private val data = Wire(Vec(numberOfStages, Vec(numberOfInputsInt, UInt(w.W))))

  for (stage <- 0 until numberOfStages) {
    val numberOfOutputs = numberOfInputsInt >> stage
    if (toPrint) println("numberOfOutputs: " + numberOfOutputs)

    if (stage == 0) { // first stage
      for (adder <- 0 until numberOfInputsInt) {
        if (adder < numberOfInputs) { // connect to input channel if there are still inputs
          data(stage)(adder) := io.inputChannel.bits(adder)
          if (toPrint) println("Connected (stage, adder): (" + stage + ", " + adder + ")" + " to input channel " + adder)
        } else { // connect to 0 if there are no more inputs
          data(stage)(adder) := 0.U
          if (toPrint) println("Connected (stage, adder): (" + stage + ", " + adder + ")" + " to 0")
        }
      }
    } else {
      for (adder <- 0 until numberOfInputsInt) {
        if (adder < numberOfOutputs) { // connect to previous stage if there are still outputs from the previous stage
          data(stage)(adder) := RegNext(data(stage - 1)(adder * 2) + data(stage - 1)(adder * 2 + 1))
          if (toPrint) println("Connected (stage, adder): (" + stage + ", " + adder + ")" + " to previous stage " + (adder * 2) + " and " + (adder * 2 + 1))
        } else { // connect to 0 if there are no more outputs from the previous stage
          data(stage)(adder) := 0.U
          if (toPrint) println("Connected (stage, adder): (" + stage + ", " + adder + ")" + " to 0")
        }
      }
    }
  }

  private val cyclesUntilOutputValid: Int = numberOfStages - 1 // number of cycles until the adder tree is done and the result is valid
  if (toPrint) println("cyclesUntilOutputValid: " + cyclesUntilOutputValid)
  private val interfaceFSM = Module(new CalculationDelayInterfaceFSM)
  interfaceFSM.io.inputValid := io.inputChannel.valid
  interfaceFSM.io.outputReady := io.outputChannel.ready
  interfaceFSM.io.doneWithCalculation := timer(cyclesUntilOutputValid, interfaceFSM.io.calculateStart)

  io.outputChannel.valid := interfaceFSM.io.outputValid
  io.inputChannel.ready := interfaceFSM.io.inputReady

  val buffer = RegInit(0.U(w.W))
  when(interfaceFSM.io.storeResult) {
    buffer := data(numberOfStages - 1)(0)
  }
  io.outputChannel.bits := buffer
}
