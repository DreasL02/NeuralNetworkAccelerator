import activation_functions.Rectifier
import chisel3._
import systolic_array.BufferedSystolicArray
import scala_utils.Optional.optional

class LayerCalculator(
                       w: Int = 8,
                       wResult: Int = 32,
                       numberOfRows: Int = 4, // number of rows in the result matrix
                       numberOfColumns: Int = 4, // number of columns in the result matrix
                       commonDimension: Int = 4, // number of columns in the first matrix and number of rows in the second matrix
                       signed: Boolean = true,
                       fixedPoint: Int = 0,
                       enableDebuggingIO: Boolean = true // enable debug signals for testing
                     ) extends Module {
  val io = IO(new Bundle {
    val load = Input(Bool()) // load values

    val inputs = Input(Vec(numberOfRows, Vec(commonDimension, UInt(w.W)))) // should only be used when load is true
    val weights = Input(Vec(numberOfColumns, Vec(commonDimension, UInt(w.W)))) // should only be used when load is true

    val biases = Input(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))) // should only be used when load is true

    val valid = Output(Bool()) // indicates that the systolic array should be done
    val result = Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W)))) // result of layer

    val debugInputs = optional(enableDebuggingIO, Output(Vec(numberOfRows, UInt(w.W))))
    val debugWeights = optional(enableDebuggingIO, Output(Vec(numberOfColumns, UInt(w.W))))
    val debugSystolicArrayResults = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))))
    val debugBiases = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))))
    val debugRounderInputs = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))))
    val debugReLUInputs = optional(enableDebuggingIO, Output(Vec(numberOfRows, Vec(numberOfColumns, UInt(wResult.W)))))
  })

  val bufferedSystolicArray = Module(new BufferedSystolicArray(w, wResult, numberOfRows, numberOfColumns, commonDimension, signed, enableDebuggingIO))
  bufferedSystolicArray.io.load := io.load
  bufferedSystolicArray.io.inputs := io.inputs
  bufferedSystolicArray.io.weights := io.weights
  io.valid := bufferedSystolicArray.io.valid

  if (enableDebuggingIO) {
    io.debugInputs.get := bufferedSystolicArray.io.debugInputs.get
    io.debugWeights.get := bufferedSystolicArray.io.debugWeights.get
    io.debugSystolicArrayResults.get := bufferedSystolicArray.io.debugSystolicArrayResults.get
  }


  // Addition of biases
  val adders = Module(new Adders(wResult, numberOfRows, numberOfColumns))
  adders.io.values := bufferedSystolicArray.io.result

  for (row <- 0 until numberOfRows) {
    for (column <- 0 until numberOfColumns) {
      // Continuously emit bias values
      val biasReg = RegInit(0.U(wResult.W))
      when(io.load) {
        biasReg := io.biases(row)(column) // replace bias value
      }

      adders.io.biases(row)(column) := biasReg
      if (enableDebuggingIO) {
        io.debugBiases.get(row)(column) := biasReg
      }
    }
  }


  if (enableDebuggingIO) {
    io.debugRounderInputs.get := adders.io.result
  }

  val rounder = Module(new Rounder(w, wResult, numberOfRows, numberOfColumns, signed, fixedPoint))
  rounder.io.input := adders.io.result

  if (enableDebuggingIO) {
    io.debugReLUInputs.get := rounder.io.output
  }

  // ReLU
  val rectifier = Module(new Rectifier(w, numberOfRows, numberOfColumns, signed))
  rectifier.io.values := rounder.io.output

  // Add quantization step here
  // val quantizer = Module(new Quantizer(w, dimension))
  // quantizer.io.values := rectifier.io.result
  // quantizer.io.fixedPoint := fixedPointTargetReg
  // quantizer.io.signed := signedTargetReg
  // io.result := quantizer.io.result

  // Result of computations
  io.result := rectifier.io.result
}
