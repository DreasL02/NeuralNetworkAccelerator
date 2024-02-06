package utils

import FixedPointConversion._

object MatrixUtils {
  def calculateMatrixMultiplication(m1: Array[Array[Float]], m2: Array[Array[Float]]): Array[Array[Float]] = {
    //https://en.wikipedia.org/wiki/Matrix_multiplication_algorithm
    val n = m1.length
    val m = m1(0).length
    val p = m2(0).length
    println("n: %d m: %d p: %d".format(n, m, p))
    val mr: Array[Array[Float]] = Array.fill(n, p)(0)
    for (i <- 0 until n) {
      for (j <- 0 until p) {
        var sum = 0.0f
        for (k <- 0 until m) {
          sum = sum + m1(i)(k) * m2(k)(j)
        }
        mr(i)(j) = sum
      }
    }
    mr
  }

  def calculateMatrixMultiplication(m1: Array[Array[Int]], m2: Array[Array[Int]]): Array[Array[Int]] = {
    //https://en.wikipedia.org/wiki/Matrix_multiplication_algorithm
    val n = m1.length
    val m = m1(0).length
    val p = m2(0).length

    val mr: Array[Array[Int]] = Array.fill(n, p)(0)
    for (i <- 0 until n) {
      for (j <- 0 until p) {
        var sum = 0
        for (k <- 0 until m) {
          sum = sum + m1(i)(k) * m2(k)(j)
        }
        mr(i)(j) = sum
      }
    }
    mr
  }

  def calculateMatrixAddition(m1: Array[Array[Float]], m2: Array[Array[Float]]): Array[Array[Float]] = {
    val mr: Array[Array[Float]] = Array.fill(m1.length, m2(0).length)(0)
    for (i <- m1.indices) {
      for (j <- m2.indices) {
        mr(i)(j) = m1(i)(j) + m2(i)(j)
      }
    }
    mr
  }

  def calculateMatrixAddition(m1: Array[Array[Int]], m2: Array[Array[Int]]): Array[Array[Int]] = {
    val mr: Array[Array[Int]] = Array.fill(m1.length, m2(0).length)(0)
    for (i <- m1.indices) {
      for (j <- m2.indices) {
        mr(i)(j) = m1(i)(j) + m2(i)(j)
      }
    }
    mr
  }


  def convertMatrixToMappedAMatrix(m: Array[Array[Int]]): Array[Array[Int]] = {
    val m_a: Array[Array[Int]] = Array.fill(m.length, m(0).length * m(0).length)(0)
    for (i <- m.indices) {
      for (j <- m(0).indices) {
        m_a(i)(m(0).length * m(0).length - 1 - i - j) = m(i)(m(0).length - 1 - j)
      }
    }
    m_a
  }

  def convertMatrixToMappedBMatrix(m: Array[Array[Int]]): Array[Array[Int]] = {
    val m_b: Array[Array[Int]] = Array.fill(m.length * m.length, m(0).length)(0)
    for (i <- m.indices) {
      for (j <- m(0).indices) {
        m_b(m.length * m.length - 1 - i - j)(j) = m(m.length - 1 - i)(j)
      }
    }
    m_b
  }

  def convertMappedMatrixToMatrix(m: Array[Int], dimension: Int): Array[Array[Int]] = {
    val m_r: Array[Array[Int]] = Array.fill(dimension, dimension)(0)
    for (i <- 0 until dimension) {
      for (j <- 0 until dimension) {
        m_r(i)(j) = m(i * dimension + j)
      }
    }
    m_r
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


  def printMatrixMultiplication(m1: Array[Array[Int]], m2: Array[Array[Int]], mr: Array[Array[Int]], text: String): Unit = {
    println("--- %s ----".format(text))
    print(matrixToString(m1))
    println("*")
    print(matrixToString(m2))
    println("=")
    print(matrixToString(mr))
  }

  def printMatrixMultiplication(m1: Array[Array[Float]], m2: Array[Array[Float]], mr: Array[Array[Float]], text: String): Unit = {
    println("--- %s ----".format(text))
    print(matrixToString(m1))
    println("*")
    print(matrixToString(m2))
    println("=")
    print(matrixToString(mr))
  }

  def printMatrixMAC(m1: Array[Array[Int]], m2: Array[Array[Int]], ma: Array[Array[Int]], mr: Array[Array[Int]], text: String): Int = {
    println("--- %s ----".format(text))
    print(matrixToString(m1))
    println("*")
    print(matrixToString(m2))
    println("+")
    print(matrixToString(ma))
    println("=")
    print(matrixToString(mr))
    1
  }

  def printMatrixMAC(m1: Array[Array[Float]], m2: Array[Array[Float]], ma: Array[Array[Float]], mr: Array[Array[Float]], text: String): Float = {
    println("--- %s ----".format(text))
    print(matrixToString(m1))
    println("*")
    print(matrixToString(m2))
    println("+")
    print(matrixToString(ma))
    println("=")
    print(matrixToString(mr))
    1.0f
  }

  def calculateMACResult(inputsFloat: Array[Array[Float]], weightsFloat: Array[Array[Float]], biasesFloat: Array[Array[Float]], fixedPoint: Int, w: Int, signed: Boolean): Array[Array[Float]] = {
    val inputsFixed = FixedPointConversion.convertFloatMatrixToFixedMatrix(inputsFloat, fixedPoint, w, signed)
    val weightsFixed = convertFloatMatrixToFixedMatrix(weightsFloat, fixedPoint, w, signed)
    val biasesFixed = convertFloatMatrixToFixedMatrix(biasesFloat, fixedPoint, w, signed)

    val inputsFloatAgain = convertFixedMatrixToFloatMatrix(inputsFixed, fixedPoint, w, signed)
    val weightsFloatAgain = convertFixedMatrixToFloatMatrix(weightsFixed, fixedPoint, w, signed)
    val biasesFloatAgain = convertFixedMatrixToFloatMatrix(biasesFixed, fixedPoint, w, signed)

    val multiplicationResultFloat = calculateMatrixMultiplication(inputsFloat, weightsFloat)
    val additionResultFloat = calculateMatrixAddition(multiplicationResultFloat, biasesFloat)
    additionResultFloat
  }
}
