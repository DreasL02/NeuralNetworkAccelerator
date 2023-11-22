package Utils

object UartCoding {

  def encodeByteToUartBits(byte: Byte): String = {
    val dataBitString = String.format("%8s", byte.toBinaryString).replace(' ', '0')

    // The data bits are sent LSB first.
    val dataBitStringLsbFirst = dataBitString.reverse
    // One start bit (0), data (8 bits), two stop bits (11).
    return "11110" + dataBitStringLsbFirst + "11111"
  }

  def encodeBytesToUartBits(bytes: Array[Byte]): String = {
    val dataBitStrings = bytes.map(encodeByteToUartBits)
    val combinedDataBitString = dataBitStrings.mkString
    return combinedDataBitString
  }

  def decodeUartBitsToString(bits: Array[BigInt], bufferBitSize: Int = 8): String = {
    var output = ""
    var i = 0

    while (i < bits.length) {
      while (bits(i) == 1) {
        i += 1

        if (i == bits.length - 1) {
          return output
        }
      }
      val dataBits = bits.slice(i + 1, i + bufferBitSize)
      val dataAsUInt = dataBits.zipWithIndex.map { case (element, index) => element * Math.pow(2, index).toInt }
      output = output + dataAsUInt.sum.toChar
      i = i + (bufferBitSize + 1)
    }

    return output
  }

}
