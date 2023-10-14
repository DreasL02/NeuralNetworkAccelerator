package Utils

object MatrixUtils {

  def calculateMatrixMultiplication(m1: Array[Array[Float]], m2: Array[Array[Float]]): Array[Array[Float]] = {
    //https://en.wikipedia.org/wiki/Matrix_multiplication_algorithm
    val mr: Array[Array[Float]] = Array.fill(m1.length, m2(0).length)(0)
    for (i <- m1.indices) {
      for (j <- m2.indices) {
        var sum = 0f
        for (k <- m2(0).indices) {
          sum = sum + m1(i)(k) * m2(k)(j)
        }
        mr(i)(j) = sum
      }
    }
    mr
  }

  def calculateMatrixMultiplication(m1: Array[Array[Int]], m2: Array[Array[Int]]): Array[Array[Int]] = {
    //https://en.wikipedia.org/wiki/Matrix_multiplication_algorithm
    val mr: Array[Array[Int]] = Array.fill(m1.length, m2(0).length)(0)
    for (i <- m1.indices) {
      for (j <- m2.indices) {
        var sum = 0
        for (k <- m2(0).indices) {
          sum = sum + m1(i)(k) * m2(k)(j)
        }
        mr(i)(j) = sum
      }
    }
    mr
  }

  def convertFloatMatrixToFixedMatrix(mf: Array[Array[Float]], fixedPointFractionBits: Int): Array[Array[Int]] = {
    val m: Array[Array[Int]] = Array.fill(mf.length, mf(0).length)(0)
    for (i <- mf.indices) {
      for (j <- mf(0).indices) {
        m(i)(j) = FixedPointConverter.floatToFixed(mf(i)(j), fixedPointFractionBits).toInt
      }
    }
    m
  }

  def convertFixedMatrixToFloatMatrix(m: Array[Array[Int]], fixedPointFractionBits: Int): Array[Array[Float]] = {
    val mf: Array[Array[Float]] = Array.fill(m.length, m(0).length)(0)
    for (i <- m.indices) {
      for (j <- m(0).indices) {
        mf(i)(j) = FixedPointConverter.fixedToFloat(m(i)(j), fixedPointFractionBits)
      }
    }
    mf
  }

  def convertMatrixToMappedAMatrix(m: Array[Array[Int]]): Array[Array[Int]] = {
    val m_a: Array[Array[Int]] = Array.fill(m(0).length, m.length * m.length)(0)
    for (i <- m.indices) {
      for (j <- m(0).indices) {
        m_a(i)(m.length * m.length - 1 - i - j) = m(i)(m.length - 1 - j)
      }
    }
    m_a
  }

  def convertMatrixToMappedBMatrix(m: Array[Array[Int]]): Array[Array[Int]] = {
    val m_b: Array[Array[Int]] = Array.fill(m(0).length * m(0).length, m.length)(0)
    for (i <- m.indices) {
      for (j <- m(0).indices) {
        m_b(m.length * m.length - 1 - i - j)(j) = m(m.length - 1 - i)(j)
      }
    }
    m_b
  }

  def matrixToString(m: Array[Array[Float]]): String = {
    var str = ""
    for (i <- m.indices) {
      for (j <- m(0).indices) {
        str = str + "%f ".format(m(i)(j))
      }
      str = str + "\n"
    }
    str
  }

  def matrixToString(m: Array[Array[Int]]): String = {
    var str = ""
    for (i <- m.indices) {
      for (j <- m(0).indices) {
        str = str + "%d ".format(m(i)(j))
      }
      str = str + "\n"
    }
    str
  }
}
