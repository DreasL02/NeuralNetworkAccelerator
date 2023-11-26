import chisel3.emitVerilog
import communication.chisel.lib.uart.{UartTx}
import systolic_array.SystolicArray


object Main extends App {
  val frequency = 50000000 * 2
  val baudRate = 115200
  val w = 8

  val dimension = 3

  val inputsL1: Array[Array[Float]] = Array(Array(1.2f, 1.3f, 2.4f), Array(0.9f, 3.4f, 0.9f), Array(2.2f, 1.2f, 0.9f))
  val weightsL1: Array[Array[Float]] = Array(Array(2.2f, 1.3f, 1.0f), Array(4.9f, 0.4f, 4.8f), Array(2.2f, 1.2f, 0.9f))
  val biasesL1: Array[Array[Float]] = Array(Array(1.0f, 1.0f, 1.0f), Array(1.0f, 1.0f, 1.0f), Array(1.0f, 1.0f, 1.0f))
  val signL1: Int = 0
  val fixedPointL1: Int = 1

  val inputsL2: Array[Array[Float]] = Array(Array(0.0f, 0.0f, 0.0f), Array(0.0f, 0.0f, 0.0f), Array(0.0f, 0.0f, 0.0f))
  val weightsL2: Array[Array[Float]] = Array(Array(1.0f, 0.9f, 0.8f), Array(0.7f, 0.6f, 0.4f), Array(0.3f, 0.2f, 0.1f))
  val biasesL2: Array[Array[Float]] = Array(Array(0.0f, 0.0f, 0.0f), Array(1.0f, 1.0f, 1.0f), Array(1.0f, 1.0f, 1.0f))
  val signL2: Int = 0
  val fixedPointL2: Int = 1


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


  emitVerilog(new IdealAccelerator(w, dimension, frequency, baudRate, mappedInputs, mappedWeights, mappedBiases, signs, fixedPoints))
}


/*

object Main extends App {
  print("UART \n")
  emitVerilog(new UartTx(50000000 * 2, 115200))
}
*/
