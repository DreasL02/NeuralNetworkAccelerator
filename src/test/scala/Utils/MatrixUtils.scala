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

  def convertFloatMatrixToFixedMatrix(mf: Array[Array[Float]], fixedPointFractionBits: Int): Array[Array[Int]] = {
    val m: Array[Array[Int]] = Array.fill(mf.length, mf(0).length)(0)
    for (i <- mf.indices) {
      for (j <- mf(0).indices) {
        m(i)(j) = FixedPointConverter.floatToFixed(mf(i)(j), fixedPointFractionBits).toInt
      }
    }
    m
  }

  def convertFloatMatrixToFixedMatrix(mf: Array[Array[Float]], fixedPointFractionBits: Int, w : Int): Array[Array[Int]] = {
    val m: Array[Array[Int]] = Array.fill(mf.length, mf(0).length)(0)
    for (i <- mf.indices) {
      for (j <- mf(0).indices) {
        m(i)(j) = FixedPointConverter.floatToFixed(mf(i)(j), fixedPointFractionBits).toInt
        if(mf(i)(j) < 0 && m(i)(j) <= 0){
          m(i)(j) = Math.pow(2, w).toInt + m(i)(j)
        }
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

  def calculateMACResult(inputsFloat: Array[Array[Float]], weightsFloat: Array[Array[Float]], biasesFloat: Array[Array[Float]], fixedPoint: Int): Array[Array[Float]] = {
    val inputsFixed = convertFloatMatrixToFixedMatrix(inputsFloat, fixedPoint)
    val weightsFixed = convertFloatMatrixToFixedMatrix(weightsFloat, fixedPoint)
    val biasesFixed = convertFloatMatrixToFixedMatrix(biasesFloat, fixedPoint)

    val inputsFloatAgain = convertFixedMatrixToFloatMatrix(inputsFixed, fixedPoint)
    val weightsFloatAgain = convertFixedMatrixToFloatMatrix(weightsFixed, fixedPoint)
    val biasesFloatAgain = convertFixedMatrixToFloatMatrix(biasesFixed, fixedPoint)

    val multiplicationResultFloat = calculateMatrixMultiplication(inputsFloat, weightsFloat)
    val additionResultFloat = calculateMatrixAddition(multiplicationResultFloat, biasesFloat)
    additionResultFloat
  }
}
