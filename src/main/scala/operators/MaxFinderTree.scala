package operators

import chisel3._
import chisel3.util.{DecoupledIO, log2Ceil}
import module_utils.CalculationDelayInterfaceFSM
import module_utils.SmallModules._

// General design is inspired by the approach presented at
// https://github.com/pConst/basic_verilog/blob/master/adder_tree.sv (visited on 08-04-2024)
// by Konstantin Pavlov
// The code is licensed under CC BY-SA 4_0 (https://creativecommons.org/licenses/by-sa/4.0/).

class MaxFinderTree(
                     w: Int,
                     numberOfInputs: Int, // number of columns in the first matrix and number of rows in the second matrix
                     toPrint: Boolean = false,
                     signed: Boolean = false
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
      for (maxer <- 0 until numberOfInputsInt) {
        if (maxer < numberOfInputs) { // connect to input channel if there are still inputs
          data(stage)(maxer) := io.inputChannel.bits(maxer)
          if (toPrint) println("Connected (stage, adder): (" + stage + ", " + maxer + ")" + " to input channel " + maxer)
        } else { // connect to 0 if there are no more inputs
          data(stage)(maxer) := 0.U
          if (toPrint) println("Connected (stage, adder): (" + stage + ", " + maxer + ")" + " to 0")
        }
      }
    } else {
      for (maxer <- 0 until numberOfInputsInt) {
        if (maxer < numberOfOutputs) { // connect to previous stage if there are still outputs from the previous stage
          val inputs = Wire(Vec(2, UInt(w.W)))
          inputs(0) := data(stage - 1)(maxer * 2)
          inputs(1) := data(stage - 1)(maxer * 2 + 1)
          val max = Wire(UInt(w.W))
          if (signed) {
            max := inputs.reduce((a, b) => Mux(a.asSInt > b.asSInt, a, b))
          } else {
            max := inputs.reduce((a, b) => Mux(a > b, a, b))
          }
          data(stage)(maxer) := RegNext(max)
          if (toPrint) println("Connected (stage, max): (" + stage + ", " + maxer + ")" + " to previous stage " + (maxer * 2) + " and " + (maxer * 2 + 1))
        } else { // connect to 0 if there are no more outputs from the previous stage
          data(stage)(maxer) := 0.U
          if (toPrint) println("Connected (stage, max): (" + stage + ", " + maxer + ")" + " to 0")
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
