package Utils

object UartCoding {

  def encodeByteToUartBits(byte: Byte): String = {
    // One start bit (0), data, two stop bits (11).
    // The data bits are sent LSB first.
    val dataBitString = String.format("%8s", byte.toBinaryString).replace(' ', '0')
    return "0" + dataBitString.reverse + "11"

  }

  def encodeBytesToUartBits(bytes: Array[Byte]): String = {
    val strings = bytes.map(encodeByteToUartBits)
    return strings.mkString
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
