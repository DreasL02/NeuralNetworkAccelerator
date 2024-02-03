import chisel3._
import chisel3.util.log2Ceil

// Holds the addresses for the memories and increments them when needed and wraps around when the last address is reached
class AddressManager(numberOfLayers: Int = 4) extends Module {
  val io = IO(new Bundle {
    val address = Output(UInt(log2Ceil(numberOfLayers).W))

    val incrementAddress = Input(Bool())
  })

  val addressReg = RegInit(0.U(log2Ceil(numberOfLayers).W))

  when(io.incrementAddress) {
    addressReg := addressReg + 1.U
  }

  when(addressReg === numberOfLayers.U) {
    addressReg := 0.U
  }

  when(addressReg === numberOfLayers.U) {
    addressReg := 0.U
  }

  io.address := addressReg
}
