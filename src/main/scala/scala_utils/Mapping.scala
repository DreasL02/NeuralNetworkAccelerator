package scala_utils

object Mapping {

  // Various functions to convert between different representations of the inputs, weights, and biases

  def mapInputs(inputs: Array[Array[Array[Byte]]]): Array[Byte] = {
    val mappedInputs = Array.fill(inputs.length * inputs(0).length * inputs(0)(0).length)(0.toByte)
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

  def mapInputs(inputs: Array[Array[Array[BigInt]]]): Array[Array[BigInt]] = {
    val mappedInputs = Array.ofDim[BigInt](inputs(0).length * inputs(0)(0).length, inputs.length)
    for (i <- inputs.indices) { //layer
      var address = 0
      for (j <- inputs(0).indices) { //row
        for (k <- inputs(0)(0).indices) { //column
          mappedInputs(address)(i) = inputs(i)(j)(inputs(0)(0).length - 1 - k)
          address = address + 1
        }
      }
    }
    mappedInputs
  }

  def mapWeights(weights: Array[Array[Array[BigInt]]]): Array[Array[BigInt]] = {
    val mappedWeights = Array.ofDim[BigInt](weights(0).length * weights(0)(0).length, weights.length)
    for (i <- weights.indices) { //layer
      var address = 0
      for (j <- weights(0).indices) { //row
        for (k <- weights(0)(0).indices) { //column
          mappedWeights(address)(i) = weights(i)(weights(0).length - 1 - k)(j)
          address = address + 1
        }
      }
    }
    mappedWeights
  }

  def mapBiases(biases: Array[Array[Array[BigInt]]]): Array[Array[BigInt]] = {
    val mappedBiases = Array.ofDim[BigInt](biases(0).length * biases(0)(0).length, biases.length)
    for (i <- biases.indices) { //layer
      var address = 0
      for (j <- biases(0).indices) { //row
        for (k <- biases(0)(0).indices) { //column
          mappedBiases(address)(i) = biases(i)(j)(k)
          address = address + 1
        }
      }
    }
    mappedBiases
  }
}
