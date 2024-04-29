package scala_utils

object FixedPointConversion {
  def fixedToFloat(fixedRepresentation: Int, fixedPointFractionalBits: Int, width: Int, signed: Boolean): Float = {
    fixedToFloat(BigDecimal.valueOf(fixedRepresentation).toBigInt, fixedPointFractionalBits, width, signed)
  }

  def fixedToFloat(fixedRepresentation: BigInt, fixedPointFractionalBits: Int, width: Int, signed: Boolean): Float = {
    var scaledToFloat = fixedRepresentation
    if (signed) {
      while (scaledToFloat >= BigInt(Math.pow(2, width - 1).toLong)) {
        scaledToFloat = scaledToFloat - BigInt(Math.pow(2, width).toLong)
      }
    }
    scaledToFloat.toFloat / (1 << fixedPointFractionalBits).toFloat
  }

  def floatToFixed(floatRepresentation: Float, fixedPointFractionalBits: Int, width: Int, signed: Boolean): BigInt = {
    var scaledToFixed: BigInt = (floatRepresentation * (1 << fixedPointFractionalBits)).round
    if (signed) {
      if (scaledToFixed > (1 << (width - 1)) - 1) {
        scaledToFixed = (1 << (width - 1)) - 1
      } else if (scaledToFixed < -(1 << (width - 1))) {
        scaledToFixed = -(1 << (width - 1))
      }

      if (scaledToFixed < 0) {
        scaledToFixed = (1 << width) + scaledToFixed
      }
    } else {
      if (scaledToFixed > (1 << width) - 1) {
        scaledToFixed = (1 << width) - 1
      } else if (scaledToFixed < 0) {
        scaledToFixed = 0
      }
    }
    scaledToFixed
  }

  def convertFloatMatrixToFixedMatrix(mf: Array[Array[Float]], fixedPointFractionBits: Int, width: Int, signed: Boolean): Array[Array[BigInt]] = {
    val m: Array[Array[BigInt]] = Array.fill(mf.length, mf(0).length)(0)
    for (i <- mf.indices) {
      for (j <- mf(0).indices) {
        m(i)(j) = floatToFixed(mf(i)(j), fixedPointFractionBits, width, signed)
      }
    }
    m
  }

  def convertFixedMatrixToFloatMatrix(m: Array[Array[BigInt]], fixedPointFractionBits: Int, width: Int, signed: Boolean): Array[Array[Float]] = {
    val mf: Array[Array[Float]] = Array.fill(m.length, m(0).length)(0)
    for (i <- m.indices) {
      for (j <- m(0).indices) {
        mf(i)(j) = fixedToFloat(m(i)(j), fixedPointFractionBits, width, signed)
      }
    }
    mf
  }


  def convertFixedTensorToFloatTensor(m: Array[Array[Array[Array[BigInt]]]], fixedPointFractionBits: Int, width: Int, signed: Boolean): Array[Array[Array[Array[Float]]]] = {
    val mf: Array[Array[Array[Array[Float]]]] = Array.fill(m.length, m(0).length, m(0)(0).length, m(0)(0)(0).length)(0)
    for (i <- m.indices) {
      for (j <- m(0).indices) {
        for (k <- m(0)(0).indices) {
          for (l <- m(0)(0)(0).indices) {
            mf(i)(j)(k)(l) = fixedToFloat(m(i)(j)(k)(l), fixedPointFractionBits, width, signed)
          }
        }
      }
    }
    mf
  }

  def convertFloatTensorToFixedTensor(mf: Array[Array[Array[Array[Float]]]], fixedPointFractionBits: Int, width: Int, signed: Boolean): Array[Array[Array[Array[BigInt]]]] = {
    val m: Array[Array[Array[Array[BigInt]]]] = Array.fill(mf.length, mf(0).length, mf(0)(0).length, mf(0)(0)(0).length)(0)
    for (i <- mf.indices) {
      for (j <- mf(0).indices) {
        for (k <- mf(0)(0).indices) {
          for (l <- mf(0)(0)(0).indices) {
            m(i)(j)(k)(l) = floatToFixed(mf(i)(j)(k)(l), fixedPointFractionBits, width, signed)
          }
        }
      }
    }
    m
  }

  def convertIntTensorToFixedTensor(mf: Array[Array[Array[Array[Int]]]], fixedPointFractionBits: Int, width: Int, signed: Boolean): Array[Array[Array[Array[BigInt]]]] = {
    val m: Array[Array[Array[Array[BigInt]]]] = Array.fill(mf.length, mf(0).length, mf(0)(0).length, mf(0)(0)(0).length)(0)
    for (i <- mf.indices) {
      for (j <- mf(0).indices) {
        for (k <- mf(0)(0).indices) {
          for (l <- mf(0)(0)(0).indices) {
            m(i)(j)(k)(l) = floatToFixed(mf(i)(j)(k)(l).toFloat, fixedPointFractionBits, width, signed)
          }
        }
      }
    }
    m
  }
}
