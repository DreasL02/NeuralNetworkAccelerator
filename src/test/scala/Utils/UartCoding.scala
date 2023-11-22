package Utils

object UartCoding {

  def encodeByteToUartDataBits(byte: Byte): String = {
    // The data bits are sent LSB first.
    val dataBitString = String.format("%8s", byte.toBinaryString).replace(' ', '0')
    return dataBitString.reverse

  }

  def encodeBytesToUartBits(bytes: Array[Byte]): String = {
    val dataBitStrings = bytes.map(encodeByteToUartDataBits)
    val combinedDataBitString = dataBitStrings.mkString
    // One start bit (0), data (variable length, multiples of 8 bits), two stop bits (11).
    return "0" + combinedDataBitString + "11"
  }

  def decodeUartBitsToString(bits: Array[BigInt]): String = {
    var output = ""
    var i = 0

    while (i < bits.length) {
      while (bits(i) == 1) {
        i += 1

        if (i == bits.length - 1) {
          return output
        }
      }
      val dataBits = bits.slice(i + 1, i + 8)
      val dataAsUInt = dataBits.zipWithIndex.map { case (element, index) => element * Math.pow(2, index).toInt }
      output = output + dataAsUInt.sum.toChar
      i = i + 9
    }

    return output
  }

}
