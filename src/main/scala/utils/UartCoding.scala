package utils

import scala.collection.mutable.ListBuffer

object UartCoding {

  def cyclesPerSerialBit(frequency: Int, baudRate: Int): Int = {
    (frequency + baudRate / 2) / baudRate - 1
  }

  def encodeByteToUartBits(byte: Byte): String = {
    val dataBitString = String.format("%8s", byte.toBinaryString).replace(' ', '0')

    // The data bits are sent LSB first.
    val dataBitStringLsbFirst = dataBitString.reverse
    // One start bit (0), data (8 bits), two stop bits (11).
    "0" + dataBitStringLsbFirst + "11"
  }

  def encodeBytesToUartBits(bytes: Array[Byte]): String = {
    val dataBitStrings = bytes.map(encodeByteToUartBits)
    val combinedDataBitString = dataBitStrings.mkString
    combinedDataBitString
  }

  def decodeUartBitsToByteArray(bits: Array[BigInt], bufferBitSize: Int = 8): Array[Byte] = {
    var output = ListBuffer[Byte]()
    var i = 0

    while (i < bits.length) {
      while (bits(i) == 1) {
        i += 1

        if (i == bits.length - 1) {
          return output.toArray
        }
      }
      val dataBits = bits.slice(i + 1, i + bufferBitSize)
      val dataAsUInt = dataBits.zipWithIndex.map { case (element, index) => element * Math.pow(2, index).toInt }
      output.append(dataAsUInt.sum.toByte)
      i = i + (bufferBitSize + 1)
    }

    output.toArray
  }

}
