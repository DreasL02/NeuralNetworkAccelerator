package operators

import chisel3._
import chisel3.util.DecoupledIO
import module_utils.NoCalculationDelayInterfaceFSM
import module_utils.SmallModules.{mult, timer}

class ComponentMultiplier(
                           w: Int,
                           wResult: Int,
                           numberOfRows: Int, // number of rows in the result matrix / number of rows in the first matrix
                           numberOfColumns: Int, // number of columns in the result matrix / number of columns in the second matrix
                           commonDimension: Int, // number of columns in the first matrix and number of rows in the second matrix
                           signed: Boolean
                         ) extends Module {
  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(numberOfRows, Vec(commonDimension, UInt(w.W)))))
    val weightChannel = Flipped(new DecoupledIO(Vec(commonDimension, Vec(numberOfColumns, UInt(w.W)))))

    val outputChannel = new DecoupledIO(Vec(numberOfRows, Vec(numberOfColumns, Vec(commonDimension, UInt(wResult.W)))))
  })

  private val multiplicationResults = Wire(Vec(numberOfRows, Vec(numberOfColumns, Vec(commonDimension, UInt(wResult.W)))))
  for (i <- 0 until numberOfRows) {
    for (j <- 0 until numberOfColumns) {
      for (k <- 0 until commonDimension) {
        multiplicationResults(i)(j)(k) := mult(io.inputChannel.bits(i)(k), io.weightChannel.bits(k)(j), w, wResult, signed)
      }
    }
  }

  private val interfaceFSM = Module(new NoCalculationDelayInterfaceFSM)
  interfaceFSM.io.inputValid := io.inputChannel.valid && io.weightChannel.valid
  interfaceFSM.io.outputReady := io.outputChannel.ready

  io.outputChannel.valid := interfaceFSM.io.outputValid
  io.inputChannel.ready := interfaceFSM.io.inputReady
  io.weightChannel.ready := interfaceFSM.io.inputReady

  private val buffer = RegInit(VecInit.fill(numberOfRows, numberOfColumns, commonDimension)(0.U(wResult.W)))
  when(interfaceFSM.io.storeResult) {
    buffer := multiplicationResults
  }

  io.outputChannel.bits := buffer
}
