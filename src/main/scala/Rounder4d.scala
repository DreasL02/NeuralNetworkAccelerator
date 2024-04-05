import chisel3._
import chisel3.util.DecoupledIO

class Rounder4d(
                 w: Int = 8,
                 wTarget: Int = 16,
                 dimensions: (Int, Int, Int, Int) = (4, 4, 4, 4),
                 signed: Boolean = true,
                 fixedPoint: Int = 8
               ) extends Module {
  assert(w > 0, "w must be greater than 0")
  assert(wTarget >= 2 * w, "wTarget must be greater than or equal 2*w")

  def this(rounderType: onnx.Operators.RounderType) = this(rounderType.wOperands, rounderType.wResult, rounderType.dimensions, rounderType.signed, rounderType.fixedPoint)

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(dimensions._1, Vec(dimensions._2, Vec(dimensions._3, Vec(dimensions._4, UInt(w.W)))))))
    val resultChannel = new DecoupledIO(Vec(dimensions._1, Vec(dimensions._2, Vec(dimensions._3, Vec(dimensions._4, UInt(wTarget.W)))))
    )
  })

  for (i <- 0 until dimensions._1) {
    for (j <- 0 until dimensions._2) {
      for (k <- 0 until dimensions._3) {
        for (l <- 0 until dimensions._4) {
          if (signed) {
            val sign = io.inputChannel.bits(i)(j)(k)(l)(w - 1)
            if (fixedPoint == 0) {
              io.resultChannel.bits(i)(j)(k)(l) := sign ## io.inputChannel.bits(i)(j)(k)(l)(w - 1, 0)
            } else {
              // Round to nearest with round up on a tie
              io.resultChannel.bits(i)(j)(k)(l) := sign ## ((io.inputChannel.bits(i)(j)(k)(l) + (1.U << (fixedPoint.U - 1.U)).asUInt) >> fixedPoint.U)(w - 1, 0).asUInt
            }
          } else {
            if (fixedPoint == 0) {
              // No fixed point, just pass through the bottom bits
              io.resultChannel.bits(i)(j)(k)(l) := io.inputChannel.bits(i)(j)(k)(l)
            } else {
              // Round to nearest with round up on a tie
              io.resultChannel.bits(i)(j)(k)(l) := ((io.inputChannel.bits(i)(j)(k)(l) + (1.U << (fixedPoint.U - 1.U)).asUInt) >> fixedPoint.U).asUInt
            }
          }
        }
      }
    }
  }

  io.resultChannel.valid := io.inputChannel.valid // Output is valid as soon as input is valid
  io.inputChannel.ready := io.resultChannel.ready && io.resultChannel.valid // Ready to receive new inputs when the result channel is ready and valid
}
