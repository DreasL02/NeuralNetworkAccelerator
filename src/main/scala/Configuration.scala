object Configuration {

  // Various functions to convert between different representations of the inputs, weights, and biases

  def mapInputs(inputs: Array[Array[Array[Int]]]): Array[Int] = {
    val mappedInputs = Array.fill(inputs.length * inputs(0).length * inputs(0)(0).length)(0)
    var address = 0
    for (i <- inputs.indices) { //layer
      for (j <- inputs(0).indices) { //row
        for (k <- inputs(0)(0).indices) { //column
          mappedInputs(address) = inputs(i)(j)(inputs(0)(0).length - 1 - k)
          address = address + 1
        }
      }
    }
    mappedInputs
  }

  def mapWeights(weights: Array[Array[Array[Int]]]): Array[Int] = {
    val mappedWeights = Array.fill(weights.length * weights(0).length * weights(0)(0).length)(0)
    var address = 0
    for (i <- weights.indices) { //layer
      for (j <- weights(0).indices) { //row
        for (k <- weights(0)(0).indices) { //column
          mappedWeights(address) = weights(i)(weights(0).length - 1 - k)(j)
          address = address + 1
        }
      }
    }
    mappedWeights
  }

  def mapBiases(biases: Array[Array[Array[Int]]]): Array[Int] = {
    val mappedBiases = Array.fill(biases.length * biases(0).length * biases(0)(0).length)(0)
    var address = 0
    for (i <- biases.indices) { //layer
      for (j <- biases(0).indices) { //row
        for (k <- biases(0)(0).indices) { //column
          mappedBiases(address) = biases(i)(j)(k)
          address = address + 1
        }
      }
    }
    mappedBiases
  }


  def floatToFixed(floatRepresentation: Float, fixedPointFractionalBits: Int): BigInt = {
    val scaledToFixed = floatRepresentation * (1 << fixedPointFractionalBits)
    BigDecimal.valueOf(scaledToFixed).toBigInt
  }

  def convertFloatMatrixToFixedMatrix(mf: Array[Array[Float]], fixedPointFractionBits: Int): Array[Array[Int]] = {
    val m: Array[Array[Int]] = Array.fill(mf.length, mf(0).length)(0)
    for (i <- mf.indices) {
      for (j <- mf(0).indices) {
        m(i)(j) = floatToFixed(mf(i)(j), fixedPointFractionBits).toInt
      }
    }
    m
  }
}
