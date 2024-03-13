import chisel3._
import chisel3.util.log2Ceil

class OneAtATimeMatrixMultiplication(
                                      w: Int = 8,
                                      wResult: Int = 32,
                                      numberOfRows: Int = 4, // number of rows in the result matrix / number of rows in the first matrix
                                      numberOfColumns: Int = 4, // number of columns in the result matrix / number of columns in the second matrix
                                      commonDimension: Int = 4, // number of columns in the first matrix and number of rows in the second matrix
                                      signed: Boolean = true,
                                      enableDebuggingIO: Boolean = true
                                    ) extends Module {

  val io = IO(new Bundle {
    val inputs = Input(Vec(numberOfRows, Vec(commonDimension, UInt(w.W)))) // should only be used when load is true
    val weights = Input(Vec(commonDimension, Vec(numberOfColumns, UInt(w.W)))) // should only be used when load is true

    val result = Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))) // result of layer

    val valid = Output(Bool()) // indicates that the systolic array should be done
    val ready = Input(Bool()) // indicates that the systolic array is ready to receive new inputs
  })

  val cyclesUntilValid: Int = numberOfColumns + numberOfRows + commonDimension - 2 // number of cycles until the systolic array is done and the result is valid

  def risingEdge(x: Bool): Bool = x && !RegNext(x) // detect rising edge

  def timer(max: Int, reset: Bool): Bool = { // timer that counts up to max and stays there until reset manually by asserting reset
    val x = RegInit(0.U(log2Ceil(max + 1).W))
    val done = x === max.U // done when x reaches max
    x := Mux(reset, 0.U, Mux(done || !io.ready, x, x + 1.U)) // reset when reset is asserted, otherwise increment if not done
    done
  }

  val load = risingEdge(io.ready) // load when io.ready goes high

  io.valid := timer(cyclesUntilValid, load) // valid when timer is done

  val flatInputs = io.inputs.flatten
  val flatWeights = io.weights.flatten
  val flatResults = RegInit(VecInit(Seq.fill(numberOfRows * numberOfColumns)(0.U(wResult.W))))

  val singleResult = Wire(UInt(wResult.W))

  val addrInputs = RegInit(0.U(log2Ceil(numberOfRows * commonDimension).W))
  val addrWeights = RegInit(0.U(log2Ceil(commonDimension * numberOfColumns).W))
  val addrResults = RegInit(0.U(log2Ceil(numberOfRows * numberOfColumns).W))


  if (signed) {
    singleResult := flatInputs(0).asSInt * flatWeights(0).asSInt + flatResults(0).asSInt
  } else {
    singleResult := flatInputs(0) * flatWeights(0) + flatResults(0)
  }


}
