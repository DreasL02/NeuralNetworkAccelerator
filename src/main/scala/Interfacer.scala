import chisel3._
import chisel3.util.DecoupledIO
import module_utils.InterfaceFSM
import module_utils.SmallModules.timer

class Interfacer(
                  val w: Int,
                  val cycles: Int
                ) extends Module {

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(UInt(w.W)))

    val resultChannel = new DecoupledIO(UInt(w.W))

    val debugComputation = Output(Bool())
    val debugDone = Output(Bool())
  })

  private val cyclesUntilOutputValid: Int = cycles

  private val interfaceFSM = Module(new InterfaceFSM)
  interfaceFSM.io.inputValid := io.inputChannel.valid //regnext
  interfaceFSM.io.outputReady := io.resultChannel.ready
  interfaceFSM.io.doneWithCalculation := timer(cyclesUntilOutputValid, interfaceFSM.io.calculateStart, interfaceFSM.io.enableTimer)

  val result = RegInit(0.U(w.W))
  when(interfaceFSM.io.calculateStart) {
    result := io.inputChannel.bits
  }.otherwise(
    result := result + 1.U
  )

  val buffer = RegInit(0.U(w.W))
  when(interfaceFSM.io.storeResult) {
    buffer := result
  }

  io.resultChannel.bits := buffer
  io.inputChannel.ready := interfaceFSM.io.inputReady
  io.resultChannel.valid := interfaceFSM.io.outputValid

  io.debugComputation := interfaceFSM.io.calculateStart
  io.debugDone := interfaceFSM.io.doneWithCalculation
}
