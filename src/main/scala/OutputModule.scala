import chisel3._
import chisel3.util.DecoupledIO

class OutputModule(
                    val w: Int,
                    val dimensions: (Int, Int, Int, Int),
                  ) extends Module {


  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(dimensions._1, Vec(dimensions._2, Vec(dimensions._3, Vec(dimensions._4, UInt(w.W)))))))
    val outputChannel = new DecoupledIO(Vec(dimensions._1, Vec(dimensions._2, Vec(dimensions._3, Vec(dimensions._4, UInt(w.W))))))
  })

  io.outputChannel <> io.inputChannel
}
