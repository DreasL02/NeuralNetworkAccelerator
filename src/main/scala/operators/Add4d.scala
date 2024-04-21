package operators

import chisel3._
import chisel3.util.DecoupledIO
import scala_utils.Optional.optional

class Add4d(
             w: Int,
             shape: (Int, Int, Int, Int),
             enableDebuggingIO: Boolean = false
           ) extends Module {

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(shape._1, Vec(shape._2, Vec(shape._3, Vec(shape._4, UInt(w.W)))))))
    val biasChannel = Flipped(new DecoupledIO(Vec(shape._1, Vec(shape._2, Vec(shape._3, Vec(shape._4, UInt(w.W)))))))

    val outputChannel = new DecoupledIO(Vec(shape._1, Vec(shape._2, Vec(shape._3, Vec(shape._4, UInt(w.W))))))

    val debugBiases = optional(enableDebuggingIO, Output(Vec(shape._1, Vec(shape._2, Vec(shape._3, Vec(shape._4, UInt(w.W)))))))
  })

  private val adds = VecInit.fill(shape._1, shape._2)(Module(new Add(w, shape._3, shape._4, enableDebuggingIO)).io)

  for (i <- 0 until shape._1) {
    for (j <- 0 until shape._2) {

      adds(i)(j).inputChannel.valid := io.inputChannel.valid
      adds(i)(j).inputChannel.bits := io.inputChannel.bits(i)(j)


      adds(i)(j).biasChannel.valid := io.biasChannel.valid
      adds(i)(j).biasChannel.bits := io.biasChannel.bits(i)(j)

      io.outputChannel.bits(i)(j) := adds(i)(j).outputChannel.bits

      if (enableDebuggingIO) {
        io.debugBiases.get(i)(j) := adds(i)(j).debugBiases.get
      }

      adds(i)(j).outputChannel.ready := io.outputChannel.ready
    }
  }

  // if one is _, then  all are _.
  io.inputChannel.ready := adds(0)(0).inputChannel.ready
  io.biasChannel.ready := adds(0)(0).biasChannel.ready
  io.outputChannel.valid := adds(0)(0).outputChannel.valid
}
