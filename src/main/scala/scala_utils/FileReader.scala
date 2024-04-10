package scala_utils

object FileReader {
  def readMatrixFromFile(fileName: String): Array[Array[Float]] = {
    val fileContents = scala.io.Source.fromFile(fileName).mkString
    val noWhiteSpaces = fileContents.filterNot(_.isWhitespace)
    val noBrackets = noWhiteSpaces.substring(1, noWhiteSpaces.length - 1)
    val rows = noBrackets.split("]")
    val matrix = rows.map(row => {
      val noFirstBracket = if (row(0) == ',') row.substring(2) else row.substring(1)
      noFirstBracket.split(",").map(_.toFloat)
    })
    matrix
  }

  def writeMatrixToFile(fileName: String, matrix: Array[Array[Float]]): Unit = {
    val file = new java.io.PrintWriter(fileName)
    file.write("[\n")
    for (i <- matrix.indices) {
      file.write("  [")
      for (j <- matrix(i).indices) {
        file.write(matrix(i)(j).toString)
        if (j < matrix(i).length - 1) {
          file.write(", ")
        }
      }
      file.write("]")
      if (i < matrix.length - 1) {
        file.write(",\n")
      }
    }
    file.write("\n]")
    file.close()
  }
}

