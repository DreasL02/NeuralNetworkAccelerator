package utils

object RandomData {

  // A function that takes two matrix dimensions and returns a random matrix of that size
  def randomMatrix(xDimension: Int, yDimension: Int, min: Float, max: Float, seed: Int): Array[Array[Float]] = {
    val matrix = Array.ofDim[Float](xDimension, yDimension)
    val random = new scala.util.Random(seed)
    for (i <- 0 until xDimension) {
      for (j <- 0 until yDimension) {
        matrix(i)(j) = min + random.nextFloat() * (max - min)
      }
    }
    matrix
  }

}

