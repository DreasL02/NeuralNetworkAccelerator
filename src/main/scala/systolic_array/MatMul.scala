package systolic_array

import chisel3._
import chisel3.util.log2Ceil
import scala_utils.Optional.optional
import module_utils.ShiftedBuffer

class MatMul(
              w: Int = 8,
              wResult: Int = 32,
              numberOfRows: Int = 4, // number of rows in the result matrix / number of rows in the first matrix
              numberOfColumns: Int = 4, // number of columns in the result matrix / number of columns in the second matrix
              commonDimension: Int = 4, // number of columns in the first matrix and number of rows in the second matrix
              signed: Boolean = true,
              enableDebuggingIO: Boolean = true
            ) extends Module {

  // Additional constructor to create a MatMul module from a MatMulType
  def this(matMulType: onnx.Operators.MatMulType, enableDebuggingIO: Boolean) = this(
    matMulType.wOperands,
    matMulType.wResult,
    matMulType.operandADimensions._1,
    matMulType.operandBDimensions._2,
    matMulType.operandADimensions._2,
    matMulType.signed,
    enableDebuggingIO
  )

  val io = IO(new Bundle {
    val inputs = Input(Vec(numberOfRows, Vec(commonDimension, UInt(w.W)))) // should only be used when load is true
    val weights = Input(Vec(numberOfColumns, Vec(commonDimension, UInt(w.W)))) // should only be used when load is true

    val result = Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))) // result of layer

    val debugInputs = optional(enableDebuggingIO, Output(Vec(numberOfRows, UInt(w.W))))
    val debugWeights = optional(enableDebuggingIO, Output(Vec(numberOfColumns, UInt(w.W))))
    val debugSystolicArrayResults = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))))

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

  val inputsBuffers = for (i <- 0 until numberOfRows) yield { // create array of buffers for inputs
    val buffer = Module(new ShiftedBuffer(w, commonDimension, i)) // shift each buffer by i to create systolic effect
    buffer // return module
  }

  val weightsBuffers = for (i <- 0 until numberOfColumns) yield { // create array of buffers for weights
    val buffer = Module(new ShiftedBuffer(w, commonDimension, i)) // shift each buffer by i to create systolic effect
    buffer // return module
  }

  val systolicArray = Module(new SystolicArray(w, wResult, numberOfRows, numberOfColumns, signed))

  // Connect buffers to signals
  for (i <- 0 until numberOfRows) {
    inputsBuffers(i).io.load := load
    inputsBuffers(i).io.data := io.inputs(i)
    systolicArray.io.a(i) := inputsBuffers(i).io.output
    if (enableDebuggingIO) {
      io.debugInputs.get(i) := inputsBuffers(i).io.output
    }
  }

  for (i <- 0 until numberOfColumns) {
    weightsBuffers(i).io.load := load
    weightsBuffers(i).io.data := io.weights(i)
    systolicArray.io.b(i) := weightsBuffers(i).io.output
    if (enableDebuggingIO) {
      io.debugWeights.get(i) := weightsBuffers(i).io.output
    }
  }

  systolicArray.io.clear := load // clear systolic array when load is asserted

  // Connect the result of the systolic array to the output
  io.result := systolicArray.io.c

  if (enableDebuggingIO) {
    io.debugSystolicArrayResults.get := systolicArray.io.c
  }
}

