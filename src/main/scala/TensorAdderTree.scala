import chisel3._
import chisel3.util.DecoupledIO
import maximum_parallel_matmul.AdderTree

class TensorAdderTree(
                       w: Int = 8,
                       numberOfInputs: Int = 4,
                       dimensionsInput: (Int, Int) = (4, 4)
                     ) extends Module {

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(numberOfInputs, Vec(dimensionsInput._1, Vec(dimensionsInput._2, UInt(w.W))))))
    val resultChannel = new DecoupledIO(Vec(dimensionsInput._1, Vec(dimensionsInput._2, UInt(w.W))))
  })

  val adderTrees = VecInit.fill(dimensionsInput._1, dimensionsInput._2)(Module(new AdderTree(w, numberOfInputs)).io)

  for (i <- 0 until dimensionsInput._1) {
    for (j <- 0 until dimensionsInput._2) {
      adderTrees(i)(j).inputChannel.valid := io.inputChannel.valid
      adderTrees(i)(j).inputChannel.bits := io.inputChannel.bits.map(_(i)(j))

      io.resultChannel.bits(i)(j) := adderTrees(i)(j).resultChannel.bits
      adderTrees(i)(j).resultChannel.ready := io.resultChannel.ready
    }
  }

  io.inputChannel.ready := adderTrees.flatten.map(_.inputChannel.ready).reduce(_ && _) && io.resultChannel.valid && io.resultChannel.ready
  io.resultChannel.valid := adderTrees.flatten.map(_.resultChannel.valid).reduce(_ && _)
}
