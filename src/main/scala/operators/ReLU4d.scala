package operators

import chisel3._
import chisel3.util.{DecoupledIO}

class ReLU4d(
              w: Int = 8,
              dimensions: (Int, Int, Int, Int) = (4, 4, 4, 4),
              signed: Boolean = true
            ) extends Module {

  val shapeOutput = (dimensions._1, dimensions._2, dimensions._3, dimensions._4)

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(dimensions._1, Vec(dimensions._2, Vec(dimensions._3, Vec(dimensions._4, UInt(w.W)))))))
    val outputChannel = new DecoupledIO(Vec(shapeOutput._1, Vec(shapeOutput._2, Vec(shapeOutput._3, Vec(shapeOutput._4, UInt(w.W))))))
  })

  private val relus = VecInit.fill(shapeOutput._1, shapeOutput._2)(Module(new ReLU(w, shapeOutput._3, shapeOutput._4)).io)

  for (i <- 0 until shapeOutput._1) {
    for (j <- 0 until shapeOutput._2) {

      relus(i)(j).inputChannel.valid := io.inputChannel.valid
      relus(i)(j).inputChannel.bits := io.inputChannel.bits(i)(j)

      io.outputChannel.bits(i)(j) := relus(i)(j).outputChannel.bits
      relus(i)(j).outputChannel.ready := io.outputChannel.ready
    }
  }

  // if one is _, then  all are _.
  io.inputChannel.ready := relus(0)(0).inputChannel.ready
  io.outputChannel.valid := relus(0)(0).outputChannel.valid

}
