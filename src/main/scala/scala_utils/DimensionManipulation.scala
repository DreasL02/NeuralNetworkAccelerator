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

  def unflatten(flattenedMatrix: Vec[UInt], numberOfRows: Int, numberOfColumns: Int, w: Int): Vec[Vec[UInt]] = {
    val matrix = Wire(Vec(numberOfRows, Vec(numberOfColumns, UInt(w.W))))
    for (row <- 0 until numberOfRows) {
      for (column <- 0 until numberOfColumns) {
        matrix(row)(column) := flattenedMatrix(row * numberOfColumns + column)
      }
    }
    matrix
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

  def reverseColumns(matrix: Vec[Vec[UInt]]): Vec[Vec[UInt]] = {
    val reversedMatrix = Wire(Vec(matrix.length, Vec(matrix(0).length, UInt(matrix(0)(0).getWidth.W))))
    for (row <- 0 until matrix.length) {
      for (column <- 0 until matrix(0).length) {
        reversedMatrix(row)(column) := matrix(row)(matrix(0).length - column - 1)
      }
    }
    reversedMatrix
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
