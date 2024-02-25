package scala_utils

object FileReader {

  def readMatrixFromFile(fileName: String): Array[Array[Float]] = {
    val bufferedSource = io.Source.fromFile(fileName)
    val matrix = bufferedSource.getLines().map(_.split(",").map(_.trim.toFloat).toArray).toArray
    bufferedSource.close
    matrix
  }

  def upscaleMatrixToDimension(mat: Array[Array[Float]], dimension: Int): Array[Array[Float]] = {
    // Take a matrix and upscale it to a given dimension, setting all new values to 0
    val newMat = Array.ofDim[Float](dimension, dimension)
    for (i <- 0 until dimension) {
      for (j <- 0 until dimension) {
        if (i < mat.length && j < mat(0).length) {
          newMat(i)(j) = mat(i)(j)
        } else {
          newMat(i)(j) = 0
        }
      }
    }
    newMat
  }

}

