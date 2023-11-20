import chisel3.emitVerilog
import communication.chisel.lib.uart.UartMain
import systolic_array.SystolicArray


object Main extends App {
  val inputsL1: Array[Array[Float]] = Array(Array(1.0f, 2.3f, 3.0f), Array(4.0f, 5.05f, 6.0f), Array(7.0f, 8.6f, 9.0f))
  val weightsL1: Array[Array[Float]] = Array(Array(1.0f, 2.3f, 3.0f), Array(4.0f, 5.05f, 6.0f), Array(7.0f, 8.6f, 9.0f))
  val biasesL1: Array[Array[Float]] = Array(Array(1.0f, 2.3f, 3.0f), Array(4.0f, 5.05f, 6.0f), Array(7.0f, 8.6f, 9.0f))
  val signL1: Int = 0
  val fixedPointL1: Int = 6

  val inputsL2: Array[Array[Float]] = Array(Array(0f, 0f, 0f), Array(4.0f, 5.05f, 6.0f), Array(7.0f, 8.6f, 9.0f))
  val weightsL2: Array[Array[Float]] = Array(Array(0f, 0f, 0f), Array(4.0f, 5.05f, 6.0f), Array(7.0f, 8.6f, 9.0f))
  val biasesL2: Array[Array[Float]] = Array(Array(0f, 0f, 0f), Array(4.0f, 5.05f, 6.0f), Array(7.0f, 8.6f, 9.0f))
  val signL2: Int = 0
  val fixedPointL2: Int = 4

  val inputs: Array[Array[Array[Int]]] = Array(
    Configuration.convertFloatMatrixToFixedMatrix(inputsL1, fixedPointL1),
    Configuration.convertFloatMatrixToFixedMatrix(inputsL2, fixedPointL2)
  )
  val weights: Array[Array[Array[Int]]] = Array(
    Configuration.convertFloatMatrixToFixedMatrix(weightsL1, fixedPointL1),
    Configuration.convertFloatMatrixToFixedMatrix(weightsL2, fixedPointL2)
  )
  val biases: Array[Array[Array[Int]]] = Array(
    Configuration.convertFloatMatrixToFixedMatrix(biasesL1, fixedPointL1),
    Configuration.convertFloatMatrixToFixedMatrix(biasesL2, fixedPointL2)
  )

  val signs: Array[Int] = Array(signL1, signL2)
  val fixedPoints: Array[Int] = Array(fixedPointL1, fixedPointL2)

  var mappedInputs = Configuration.mapInputs(inputs)
  var mappedWeights = Configuration.mapWeights(weights)
  var mappedBiases = Configuration.mapBiases(biases)

  emitVerilog(new Accelerator(w = 16, dimension = 4,
    mappedInputs, mappedWeights,
    mappedBiases, signs, fixedPoints))
}



/*
object Main extends App {
  print("UART \n")
  emitVerilog(new UartMain(50000000 * 2, 115200))
}
*/
