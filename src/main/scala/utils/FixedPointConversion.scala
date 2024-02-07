package utils

object FixedPointConversion {
  def fixedToFloat(fixedRepresentation: Int, fixedPointFractionalBits: Int, width: Int, signed: Boolean): Float = {
    fixedToFloat(BigDecimal.valueOf(fixedRepresentation).toBigInt, fixedPointFractionalBits, width, signed)
  }

  def fixedToFloat(fixedRepresentation: BigInt, fixedPointFractionalBits: Int, width: Int, signed: Boolean): Float = {
    var scaledToFloat = fixedRepresentation
    if (signed) {
      while (scaledToFloat >= Math.pow(2, width - 1).toInt) {
        scaledToFloat = scaledToFloat - Math.pow(2, width).toInt
      }
    }
    scaledToFloat.toFloat / (1 << fixedPointFractionalBits).toFloat
  }

  def floatToFixed(floatRepresentation: Float, fixedPointFractionalBits: Int, width: Int, signed: Boolean): BigInt = {
    var scaledToFixed = (floatRepresentation * (1 << fixedPointFractionalBits)).round
    if (signed) {
      if (floatRepresentation < 0 && scaledToFixed <= 0) {
        scaledToFixed = Math.pow(2, width).toInt + scaledToFixed
      }
    }
    // If the number is too large, set it to 0
    if (scaledToFixed >= Math.pow(2, width).toInt) {
      scaledToFixed = 0
    }
    BigDecimal.valueOf(scaledToFixed).toBigInt
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
}
