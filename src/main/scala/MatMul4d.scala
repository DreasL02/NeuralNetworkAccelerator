import chisel3._
import chisel3.util.DecoupledIO

class MatMul4d(
                w: Int = 8,
                wResult: Int = 32,
                dimensionsInput: (Int, Int, Int, Int) = (4, 4, 4, 4),
                dimensionsWeight: (Int, Int, Int, Int) = (4, 4, 4, 4),
                signed: Boolean = true,
                enableDebuggingIO: Boolean = true
              )
  extends Module {

  assert(dimensionsInput._1 == dimensionsWeight._1, "The first dimension of the input and weight matrices must be the same")
  assert(dimensionsInput._2 == dimensionsWeight._2, "The second dimension of the input and weight matrices must be the same")
  assert(dimensionsInput._4 == dimensionsWeight._3, "The fourth dimension of the input and third of the weight matrices must be the same")

  def this(matMulType: onnx.Operators.MatMulType, enableDebuggingIO: Boolean) = this(matMulType.wOperands, matMulType.wResult, matMulType.operandADimensions, matMulType.operandBDimensions, matMulType.signed, enableDebuggingIO)

  private val dimensionsOutput = (dimensionsInput._1, dimensionsInput._2, dimensionsInput._3, dimensionsWeight._4)

  val io = IO(new Bundle {
    val inputChannel = Flipped(new DecoupledIO(Vec(dimensionsInput._1, Vec(dimensionsInput._2, Vec(dimensionsInput._3, Vec(dimensionsInput._4, UInt(w.W)))))))
    val weightChannel = Flipped(new DecoupledIO(Vec(dimensionsWeight._1, Vec(dimensionsWeight._2, Vec(dimensionsWeight._3, Vec(dimensionsWeight._4, UInt(w.W)))))))

    val outputChannel = new DecoupledIO(Vec(dimensionsOutput._1, Vec(dimensionsOutput._2, Vec(dimensionsOutput._3, Vec(dimensionsOutput._4, UInt(wResult.W))))))
  })


  private val inputReadies = Wire(Vec(dimensionsInput._1, Vec(dimensionsInput._2, Bool())))
  private val weightReadies = Wire(Vec(dimensionsWeight._1, Vec(dimensionsWeight._2, Bool())))
  private val outputValids = Wire(Vec(dimensionsOutput._1, Vec(dimensionsOutput._2, Bool())))

  private val matMuls = VecInit.fill(dimensionsOutput._1, dimensionsOutput._2)(Module(new MatMul(w, wResult, dimensionsInput._3, dimensionsWeight._4, dimensionsWeight._3, signed, enableDebuggingIO)).io)

  for (i <- 0 until dimensionsOutput._1) {
    for (j <- 0 until dimensionsOutput._2) {

      matMuls(i)(j).inputChannel.valid := io.inputChannel.valid
      matMuls(i)(j).inputChannel.bits := io.inputChannel.bits(i)(j)
      inputReadies(i)(j) := matMuls(i)(j).inputChannel.ready


      matMuls(i)(j).weightChannel.valid := io.weightChannel.valid
      matMuls(i)(j).weightChannel.bits := io.weightChannel.bits(i)(j)
      weightReadies(i)(j) := matMuls(i)(j).weightChannel.ready


      io.outputChannel.bits(i)(j) := matMuls(i)(j).resultChannel.bits
      matMuls(i)(j).resultChannel.ready := io.outputChannel.ready
      outputValids(i)(j) := matMuls(i)(j).resultChannel.valid
    }
  }

  io.inputChannel.ready := inputReadies.flatten.reduce(_ && _)
  io.weightChannel.ready := weightReadies.flatten.reduce(_ && _)
  io.outputChannel.valid := outputValids.flatten.reduce(_ && _)
}
