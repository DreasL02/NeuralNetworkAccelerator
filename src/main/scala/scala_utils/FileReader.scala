package scala_utils

object FileReader {

  // read a matrix from a file given in row major order with [] as delimiters. I.e.
  // [
  //  [
  //    1,
  //    2,
  //    3
  //  ],
  //  [
  //    4,
  //    5,
  //    6
  //  ]
  // ]
  // will be read as
  // Array(Array(1, 4), Array(2, 5), Array(3, 6))
  // White spaces are ignored and the matrix is read as a matrix of floats

  // The steps are:
  // 1. Read the file into a single string
  // 2. Remove all white spaces
  // 3. Remove the first and last character (which are '[' and ']')
  // 4. Split by ']' to get the rows
  // 5. Remove the first character of each row (which is '[') or remove the first two characters of the first row if the first character is ','
  // 6. Split by ',' to get the elements of each row
  // 7. Convert each element to a float
  // 8. Collect the elements into a matrix

  // Using these steps the function readMatrixFromFile can be implemented as follows:
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


}

