import chisel3._
import chisel3.util.DecoupledIO

class Interfacer2(
                   val w: Int,
                   val cycles1: Int,
                   val cycles2: Int
                 ) extends Module {

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(UInt(w.W)))

    val resultChannel = new DecoupledIO(UInt(w.W))

    val debugComputation1 = Output(Bool())
    val debugDone1 = Output(Bool())

    val debugComputation2 = Output(Bool())
    val debugDone2 = Output(Bool())
  })

  val interfacer1 = Module(new Interfacer(w, cycles1))
  interfacer1.io.inputChannel <> io.inputChannel

  val interfacer2 = Module(new Interfacer(w, cycles2))
  interfacer2.io.inputChannel <> interfacer1.io.resultChannel

  val interfacer3 = Module(new Interfacer(w, 0))
  interfacer3.io.inputChannel <> interfacer2.io.resultChannel

  io.resultChannel <> interfacer3.io.resultChannel

  io.debugComputation1 := interfacer1.io.debugComputation
  io.debugDone1 := interfacer1.io.debugDone

  io.debugComputation2 := interfacer2.io.debugComputation
  io.debugDone2 := interfacer2.io.debugDone
}
