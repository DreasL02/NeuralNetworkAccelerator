import scala_utils.{FixedPointConversion, Mapping}
import chisel3.emitVerilog
import communication.chisel.lib.uart.UartTx
import systolic_array.SystolicArray

object Main extends App {
  val frequency = 50000000 * 2
  val baudRate = 115200
  val w = 8
  val wStore = w * 4

  val dimension = 3

  val inputsL1: Array[Array[Float]] = Array(Array(1.2f, 1.3f, 2.4f), Array(0.9f, 3.4f, 0.9f), Array(2.2f, 1.2f, 0.9f))
  val weightsL1: Array[Array[Float]] = Array(Array(2.2f, 1.3f, 1.0f), Array(4.9f, 0.4f, 4.8f), Array(2.2f, 1.2f, 0.9f))
  val biasesL1: Array[Array[Float]] = Array(Array(1.0f, 1.0f, 1.0f), Array(1.0f, 1.0f, 1.0f), Array(1.0f, 1.0f, 1.0f))
  val signL1: Int = 0
  val fixedPointL1: Int = 0

  val inputsL2: Array[Array[Float]] = Array(Array(9.0f, 8.0f, 7.0f), Array(6.0f, 5.0f, 4.0f), Array(3.0f, 2.0f, 1.0f))
  val weightsL2: Array[Array[Float]] = Array(Array(1.0f, 0.9f, 0.8f), Array(0.7f, 0.6f, 0.4f), Array(0.3f, 0.2f, 0.1f))
  val biasesL2: Array[Array[Float]] = Array(Array(0.0f, 0.0f, 0.0f), Array(1.0f, 1.0f, 1.0f), Array(1.0f, 1.0f, 1.0f))
  val signL2: Int = 0
  val fixedPointL2: Int = 0

  val inputsL3: Array[Array[Float]] = Array(Array(1.0f, 1.0f, 1.0f), Array(0.0f, 0.0f, 0.0f), Array(1.0f, 1.0f, 1.0f))
  val weightsL3: Array[Array[Float]] = Array(Array(1.0f, 0.9f, 0.8f), Array(0.7f, 2f, 0.4f), Array(3f, 0.2f, 0.1f))
  val biasesL3: Array[Array[Float]] = Array(Array(1.0f, 1.0f, 1.0f), Array(1.0f, 0.0f, 1.0f), Array(1.0f, 1.0f, 1.0f))
  val signL3: Int = 0
  val fixedPointL3: Int = 0

  val inputs: Array[Array[Array[BigInt]]] = Array(
    FixedPointConversion.convertFloatMatrixToFixedMatrix(inputsL1, fixedPointL1, w, signL1 == 1),
    FixedPointConversion.convertFloatMatrixToFixedMatrix(inputsL2, fixedPointL2, w, signL2 == 1),
    FixedPointConversion.convertFloatMatrixToFixedMatrix(inputsL3, fixedPointL3, w, signL3 == 1)
  )
  val weights: Array[Array[Array[BigInt]]] = Array(
    FixedPointConversion.convertFloatMatrixToFixedMatrix(weightsL1, fixedPointL1, w, signL1 == 1),
    FixedPointConversion.convertFloatMatrixToFixedMatrix(weightsL2, fixedPointL2, w, signL2 == 1),
    FixedPointConversion.convertFloatMatrixToFixedMatrix(weightsL3, fixedPointL3, w, signL3 == 1)
  )
  val biases: Array[Array[Array[BigInt]]] = Array(
    FixedPointConversion.convertFloatMatrixToFixedMatrix(biasesL1, fixedPointL1, wStore, signL1 == 1),
    FixedPointConversion.convertFloatMatrixToFixedMatrix(biasesL2, fixedPointL2, wStore, signL2 == 1),
    FixedPointConversion.convertFloatMatrixToFixedMatrix(biasesL3, fixedPointL3, wStore, signL3 == 1)
  )

  val signs: Array[BigInt] = Array(signL1, signL2, signL3)
  val fixedPoints: Array[BigInt] = Array(fixedPointL1, fixedPointL2, fixedPointL3)

  var mappedInputs = Mapping.mapInputs(inputs)
  var mappedWeights = Mapping.mapWeights(weights)
  var mappedBiases = Mapping.mapBiases(biases)

  //emitVerilog(new Top(w, dimension, frequency, baudRate, mappedInputs, mappedWeights, mappedBiases, signs, fixedPoints, false)) //TODO reenable
}


/*

object Main extends App {
  print("UART \n")
  emitVerilog(new UartTx(50000000 * 2, 115200))
}
*/
