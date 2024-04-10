package scala_utils

import chisel3._

object DimensionManipulation {
  def flatten(matrix: Vec[Vec[UInt]], numberOfRows: Int, numberOfColumns: Int): Vec[UInt] = {
    val flattenedMatrix = Wire(Vec(numberOfRows * numberOfColumns, UInt(matrix(0)(0).getWidth.W)))
    for (row <- 0 until numberOfRows) {
      for (column <- 0 until numberOfColumns) {
        flattenedMatrix(row * numberOfColumns + column) := matrix(row)(column)
      }
    }
    flattenedMatrix
  }

  def transpose(matrix: Vec[Vec[UInt]]): Vec[Vec[UInt]] = {
    val transposedMatrix = Wire(Vec(matrix(0).length, Vec(matrix.length, UInt(matrix(0)(0).getWidth.W))))
    for (row <- 0 until matrix.length) {
      for (column <- 0 until matrix(0).length) {
        transposedMatrix(column)(row) := matrix(row)(column)
      }
    }
    transposedMatrix
  }

  def reverseRows(matrix: Vec[Vec[UInt]]): Vec[Vec[UInt]] = {
    val reversedMatrix = Wire(Vec(matrix.length, Vec(matrix(0).length, UInt(matrix(0)(0).getWidth.W))))
    for (row <- 0 until matrix.length) {
      for (column <- 0 until matrix(0).length) {
        reversedMatrix(row)(column) := matrix(matrix.length - row - 1)(column)
      }
    }
    reversedMatrix
  }
}
