import chisel3._
import chisel3.util.DecoupledIO

class Rounder4d(
                 wBefore: Int = 8,
                 wAfter: Int = 16,
                 dimensions: (Int, Int, Int, Int) = (4, 4, 4, 4),
                 signed: Boolean = true,
                 fixedPoint: Int = 8
               ) extends Module {

  def this(rounderType: onnx.Operators.RounderType) = this(rounderType.wOperands, rounderType.wResult, rounderType.dimensions, rounderType.signed, rounderType.fixedPoint)

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(dimensions._1, Vec(dimensions._2, Vec(dimensions._3, Vec(dimensions._4, UInt(wBefore.W)))))))
    val resultChannel = new DecoupledIO(Vec(dimensions._1, Vec(dimensions._2, Vec(dimensions._3, Vec(dimensions._4, UInt(wAfter.W)))))
    )
  })

  private val rounders = VecInit.fill(dimensions._1, dimensions._2)(Module(new Rounder(wBefore, wAfter, dimensions._3, dimensions._4, signed, fixedPoint)).io)

  for (i <- 0 until dimensions._1) {
    for (j <- 0 until dimensions._2) {
      rounders(i)(j).inputChannel.valid := io.inputChannel.valid
      rounders(i)(j).inputChannel.bits := io.inputChannel.bits(i)(j)

      rounders(i)(j).resultChannel.ready := io.resultChannel.ready
      io.resultChannel.bits(i)(j) := rounders(i)(j).resultChannel.bits
    }
  }

  io.inputChannel.ready := rounders(0)(0).inputChannel.ready
  io.resultChannel.valid := rounders(0)(0).resultChannel.valid
}
