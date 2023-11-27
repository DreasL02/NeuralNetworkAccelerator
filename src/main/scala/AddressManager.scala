import chisel3._
import chisel3.util.log2Ceil

class AddressManager(dimension: Int = 4, lengthOfMatrixMemory: Int, lengthOfVectorMemory: Int) extends Module {
  val io = IO(new Bundle {
    val matrixAddress = Output(UInt(log2Ceil(lengthOfMatrixMemory).W))
    val vectorAddress = Output(UInt(log2Ceil(lengthOfVectorMemory).W))

    val incrementAddress = Input(Bool())
  })

  val matrixAddressReg = RegInit(0.U(log2Ceil(lengthOfMatrixMemory).W))
  val vectorAddressReg = RegInit(0.U(log2Ceil(lengthOfVectorMemory).W))

  when(io.incrementAddress) {
    matrixAddressReg := matrixAddressReg + dimension.U * dimension.U
  }

  when(io.incrementAddress) {
    vectorAddressReg := vectorAddressReg + 1.U
  }

  when(matrixAddressReg === lengthOfMatrixMemory.U) {
    matrixAddressReg := 0.U
  }

  when(vectorAddressReg === lengthOfVectorMemory.U) {
    vectorAddressReg := 0.U
  }

  io.matrixAddress := matrixAddressReg
  io.vectorAddress := vectorAddressReg
}
