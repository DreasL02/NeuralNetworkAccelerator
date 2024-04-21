package operators

import chisel3._
import chisel3.util.DecoupledIO

class TensorAdderTree(
                       w: Int = 8,
                       numberOfInputs: Int = 4,
                       dimensionsInput: (Int, Int) = (4, 4)
                     ) extends Module {

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(numberOfInputs, Vec(dimensionsInput._1, Vec(dimensionsInput._2, UInt(w.W))))))
    val outputChannel = new DecoupledIO(Vec(dimensionsInput._1, Vec(dimensionsInput._2, UInt(w.W))))
  })

  private val adderTrees = VecInit.fill(dimensionsInput._1, dimensionsInput._2)(Module(new AdderTree(w, numberOfInputs)).io)

  for (i <- 0 until dimensionsInput._1) {
    for (j <- 0 until dimensionsInput._2) {
      adderTrees(i)(j).inputChannel.valid := io.inputChannel.valid
      adderTrees(i)(j).inputChannel.bits := io.inputChannel.bits.map(_(i)(j))

      io.outputChannel.bits(i)(j) := adderTrees(i)(j).outputChannel.bits
      adderTrees(i)(j).outputChannel.ready := io.outputChannel.ready
    }
  }

  io.inputChannel.ready := adderTrees(0)(0).inputChannel.ready
  io.outputChannel.valid := adderTrees(0)(0).outputChannel.valid
}
