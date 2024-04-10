package ws_systolic_array

import chisel3._
import module_utils.SmallModules.mult

class ProcessingElement(
                         w: Int = 8, // width of the inputs
                         wResult: Int = 32, // width of the result / register
                         signed: Boolean = true // to determine if signed or unsigned multiplication should be used
                       ) extends Module {

  val io = IO(new Bundle {
    val inputActivationIn = Input(UInt(w.W))
    val weightPreload = Input(UInt(w.W))
    val partialSumIn = Input(UInt(wResult.W))

    val inputActivationOut = Output(UInt(w.W))
    val partialSumOut = Output(UInt(wResult.W))

    val loadWeight = Input(Bool())
    val clear = Input(Bool())
  })


  private val weightReg = RegInit(0.U(w.W))
  private val inputActivationReg = RegInit(0.U(w.W))
  private val partialSumReg = RegInit(0.U(wResult.W))

  private val multiplicationResult = mult(io.inputActivationIn, weightReg, w, wResult, signed)

  partialSumReg := multiplicationResult + io.partialSumIn
  inputActivationReg := io.inputActivationIn

  io.inputActivationOut := inputActivationReg
  io.partialSumOut := partialSumReg

  when(io.clear) {
    partialSumReg := 0.U
    inputActivationReg := 0.U
  }

  when(io.loadWeight) {
    weightReg := io.weightPreload
  }
}
