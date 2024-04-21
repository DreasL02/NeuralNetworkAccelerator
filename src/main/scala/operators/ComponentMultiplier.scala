package operators

import chisel3._
import chisel3.util.DecoupledIO
import module_utils.SmallModules.{mult, timer}

class ComponentMultiplier(
                           w: Int = 8,
                           wResult: Int = 32,
                           numberOfRows: Int = 4, // number of rows in the result matrix / number of rows in the first matrix
                           numberOfColumns: Int = 4, // number of columns in the result matrix / number of columns in the second matrix
                           commonDimension: Int = 4, // number of columns in the first matrix and number of rows in the second matrix
                           signed: Boolean = true
                         ) extends Module {
  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(numberOfRows, Vec(commonDimension, UInt(w.W)))))
    val weightChannel = Flipped(new DecoupledIO(Vec(commonDimension, Vec(numberOfColumns, UInt(w.W)))))

    val outputChannel = new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, Vec(commonDimension, UInt(wResult.W)))))
  })

  private val inputs = RegNext(io.inputChannel.bits)
  private val weights = RegNext(io.weightChannel.bits)

  private val multiplicationResults = Wire(Vec(numberOfRows, Vec(numberOfColumns, Vec(commonDimension, UInt(wResult.W)))))
  for (i <- 0 until numberOfRows) {
    for (j <- 0 until numberOfColumns) {
      for (k <- 0 until commonDimension) {
        multiplicationResults(i)(j)(k) := mult(inputs(i)(k), weights(k)(j), w, wResult, signed)
      }
    }
  }

  private val cyclesUntilOutputValid: Int = 1

  private val interfaceFSM = Module(new module_utils.InterfaceFSM)
  interfaceFSM.io.inputValid := io.inputChannel.valid && io.weightChannel.valid
  interfaceFSM.io.outputReady := io.outputChannel.ready
  interfaceFSM.io.doneWithCalculation := timer(cyclesUntilOutputValid, interfaceFSM.io.calculateStart)

  io.outputChannel.valid := interfaceFSM.io.outputValid
  io.inputChannel.ready := interfaceFSM.io.inputReady
  io.weightChannel.ready := interfaceFSM.io.inputReady

  val buffer = RegInit(VecInit.fill(numberOfRows, numberOfColumns, commonDimension)(0.U(wResult.W)))
  when(interfaceFSM.io.storeResult) {
    buffer := multiplicationResults
  }

  io.outputChannel.bits := buffer
}
