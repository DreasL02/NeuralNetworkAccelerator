package Utils

object FixedPointConverter {

  def fixedToFloat(fixedRepresentation: BigInt, fixedPointFractionalBits: Int): Float = {
    fixedRepresentation.toFloat / (1 << fixedPointFractionalBits).toFloat
  }

  def floatToFixed(floatRepresentation: Float, fixedPointFractionalBits: Int): BigInt = {
    val scaledToFixed = (floatRepresentation * (1 << fixedPointFractionalBits)).round
    BigDecimal.valueOf(scaledToFixed).toBigInt
  }

}

