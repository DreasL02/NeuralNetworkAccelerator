package scala_utils

object MatrixUtils {
  def calculateMatrixMultiplication(m1: Array[Array[Float]], m2: Array[Array[Float]]): Array[Array[Float]] = {
    // Golden model for matrix multiplication implemented as per the algorithm described at
    //https://en.wikipedia.org/wiki/Matrix_multiplication_algorithm
    val n = m1.length
    val m = m1(0).length
    val p = m2(0).length
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

  def calculateMatrixMultiplication(m1: Array[Array[BigInt]], m2: Array[Array[BigInt]]): Array[Array[BigInt]] = {
    // Golden model for matrix multiplication implemented as per the algorithm described at
    //https://en.wikipedia.org/wiki/Matrix_multiplication_algorithm
    val n = m1.length
    val m = m1(0).length
    val p = m2(0).length

    val mr: Array[Array[BigInt]] = Array.fill(n, p)(0)
    for (i <- 0 until n) {
      for (j <- 0 until p) {
        var sum: BigInt = 0
        for (k <- 0 until m) {
          sum = sum + m1(i)(k) * m2(k)(j)
        }
        mr(i)(j) = sum
      }
    }
    mr
  }

  def calculateMatrixAddition(m1: Array[Array[Float]], m2: Array[Array[Float]]): Array[Array[Float]] = {
    // should do element wise addition of two equal sized matrices
    val mr: Array[Array[Float]] = Array.fill(m1.length, m2(0).length)(0)
    for (i <- m1.indices) {
      for (j <- m2(0).indices) {
        mr(i)(j) = m1(i)(j) + m2(i)(j)
      }
    }
    mr
  }

  def calculateMatrixAddition(m1: Array[Array[BigInt]], m2: Array[Array[BigInt]]): Array[Array[BigInt]] = {
    val mr: Array[Array[BigInt]] = Array.fill(m1.length, m2(0).length)(0)
    for (i <- m1.indices) {
      for (j <- m2(0).indices) {
        mr(i)(j) = m1(i)(j) + m2(i)(j)
      }
    }
    mr
  }

  def calculateMatrixReLU(m1: Array[Array[Float]], signed: Boolean): Array[Array[Float]] = {
    val mr: Array[Array[Float]] = Array.fill(m1.length, m1(0).length)(0)
    for (i <- m1.indices) {
      for (j <- m1(0).indices) {
        if (signed && m1(i)(j) < 0) {
          mr(i)(j) = 0
        } else {
          mr(i)(j) = m1(i)(j)
        }
      }
    }
    mr
  }


  def convertMatrixToMappedAMatrix(m: Array[Array[BigInt]], increment: Int): Array[Array[BigInt]] = {
    val m_a: Array[Array[BigInt]] = Array.fill(m.length, m.length + increment)(0)
    for (i <- m.indices) {
      for (j <- m(0).indices) {
        m_a(i)(m.length + increment - 1 - i - j) = m(i)(m(0).length - 1 - j)
      }
    }
    m_a
  }

  def convertMatrixToMappedBMatrix(m: Array[Array[BigInt]], increment: Int): Array[Array[BigInt]] = {
    val m_b: Array[Array[BigInt]] = Array.fill(m(0).length + increment, m(0).length)(0)
    for (i <- m.indices) {
      for (j <- m(0).indices) {
        m_b(m(0).length + increment - 1 - i - j)(j) = m(m.length - 1 - i)(j)
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

  def matrixToString(m: Array[Array[BigInt]]): String = {
    var str = ""
    for (i <- m.indices) {
      for (j <- m(0).indices) {
        str = str + "%d ".format(m(i)(j))
      }
      str = str + "\n"
    }
    str
  }

  def tensorToString(m: Array[Array[Array[Array[BigInt]]]]): String = {
    var str = ""
    for (i <- m.indices) {
      for (j <- m(0).indices) {
        for (k <- m(0)(0).indices) {
          for (l <- m(0)(0)(0).indices) {
            str = str + "%d ".format(m(i)(j)(k)(l))
          }
          str = str + "\n"
        }
        str = str + "\n"
      }
      str = str + "\n"
    }
    str
  }


  def printMatrixMultiplication(m1: Array[Array[BigInt]], m2: Array[Array[BigInt]], mr: Array[Array[BigInt]], text: String): Unit = {
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

  def printMatrixMAC(m1: Array[Array[BigInt]], m2: Array[Array[BigInt]], ma: Array[Array[BigInt]], mr: Array[Array[BigInt]], text: String): Int = {
    println("--- %s ----".format(text))
    print(matrixToString(m1))
    println("*")
    print(matrixToString(m2))
    println("+")
    print(matrixToString(ma))
    println("=")
    print(matrixToString(mr))
    // does not include ReLU
    1
  }

  def printMatrixMAC(m1: Array[Array[Float]], m2: Array[Array[Float]], ma: Array[Array[Float]], mr: Array[Array[Float]], mre: Array[Array[Float]], text: String): Float = {
    println("--- %s ----".format(text))
    print(matrixToString(m1))
    println("*")
    print(matrixToString(m2))
    println("+")
    print(matrixToString(ma))
    println("=")
    print(matrixToString(mr))
    println("ReLU:")
    print(matrixToString(mre))
    1.0f
  }
}
