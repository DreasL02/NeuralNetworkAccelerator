package operators

import chisel3._
import chisel3.util.DecoupledIO

class MatMul4d(
                w: Int,
                wResult: Int,
                shapeInput: (Int, Int, Int, Int),
                shapeWeight: (Int, Int, Int, Int),
                signed: Boolean,
                enableDebuggingIO: Boolean = false
              )
  extends Module {

  assert(shapeInput._1 == shapeWeight._1, "The first shape of the input and weight matrices must be the same")
  assert(shapeInput._2 == shapeWeight._2, "The second shape of the input and weight matrices must be the same")
  assert(shapeInput._4 == shapeWeight._3, "The fourth shape of the input and third of the weight matrices must be the same")

  private val shapeOutput = (shapeInput._1, shapeInput._2, shapeInput._3, shapeWeight._4)

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(shapeInput._1, Vec(shapeInput._2, Vec(shapeInput._3, Vec(shapeInput._4, UInt(w.W)))))))
    val weightChannel = Flipped(new DecoupledIO(Vec(shapeWeight._1, Vec(shapeWeight._2, Vec(shapeWeight._3, Vec(shapeWeight._4, UInt(w.W)))))))

    val outputChannel = new DecoupledIO(Vec(shapeOutput._1, Vec(shapeOutput._2, Vec(shapeOutput._3, Vec(shapeOutput._4, UInt(wResult.W))))))
  })

  private val matMuls = VecInit.fill(shapeOutput._1, shapeOutput._2)(Module(new MatMul(w, wResult, shapeInput._3, shapeWeight._4, shapeWeight._3, signed, enableDebuggingIO)).io)

  for (i <- 0 until shapeOutput._1) {
    for (j <- 0 until shapeOutput._2) {
      matMuls(i)(j).inputChannel.valid := io.inputChannel.valid
      matMuls(i)(j).inputChannel.bits := io.inputChannel.bits(i)(j)

      matMuls(i)(j).weightChannel.valid := io.weightChannel.valid
      matMuls(i)(j).weightChannel.bits := io.weightChannel.bits(i)(j)

      io.outputChannel.bits(i)(j) := matMuls(i)(j).outputChannel.bits
      matMuls(i)(j).outputChannel.ready := io.outputChannel.ready
    }
  }

  // if one matmul is ready, then all matmuls are ready as they all have the same flow.
  io.inputChannel.ready := matMuls(0)(0).inputChannel.ready
  io.weightChannel.ready := matMuls(0)(0).weightChannel.ready
  // if one matmul is valid, then all matmuls are valid as they all have the same flow.
  io.outputChannel.valid := matMuls(0)(0).outputChannel.valid
}
