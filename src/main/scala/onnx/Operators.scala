package onnx

object Operators {
  case class InputType(w: Int, dimensions: (Int, Int)) {
    require(w > 0, "w must be greater than 0")
    require(dimensions._1 > 0, "dimensions._1 must be greater than 0")
    require(dimensions._2 > 0, "dimensions._2 must be greater than 0")
  }

  case class OutputType(w: Int, dimensions: (Int, Int)) {
    require(w > 0, "w must be greater than 0")
    require(dimensions._1 > 0, "dimensions._1 must be greater than 0")
    require(dimensions._2 > 0, "dimensions._2 must be greater than 0")
  }

  case class InitializerType(dimensions: (Int, Int), w: Int, data: Seq[Seq[Int]]) {
    require(w > 0, "w must be greater than 0")
    require(dimensions._1 > 0, "dimensions._1 must be greater than 0")
    require(dimensions._2 > 0, "dimensions._2 must be greater than 0")
  }

  case class AddType(wOperands: Int, operandDimensions: (Int, Int)) {
    require(wOperands > 0, "wOperands must be greater than 0")
    require(operandDimensions._1 > 0, "operandDimensions._1 must be greater than 0")
    require(operandDimensions._2 > 0, "operandDimensions._2 must be greater than 0")
  }

  case class MatMulType(wOperands: Int, wResult: Int, signed: Boolean, operandADimensions: (Int, Int), operandBDimensions: (Int, Int)) {
    require(wOperands > 0, "wOperands must be greater than 0")
    require(wResult >= 2 * wOperands, "wResult must be greater than or equal 2 * wOperands")
    require(operandADimensions._1 > 0, "operandADimensions._1 must be greater than 0")
    require(operandADimensions._2 > 0, "operandADimensions._2 must be greater than 0")
    require(operandBDimensions._1 > 0, "operandBDimensions._1 must be greater than 0")
    require(operandBDimensions._2 > 0, "operandBDimensions._2 must be greater than 0")
  }

  case class ReLUType(wOperands: Int, signed: Boolean, operandDimensions: (Int, Int)) {
    require(wOperands > 0, "wOperands must be greater than 0")
    require(operandDimensions._1 > 0, "operandDimensions._1 must be greater than 0")
    require(operandDimensions._2 > 0, "operandDimensions._2 must be greater than 0")
  }

  case class RoundType(wOperands: Int, wResult: Int, signed: Boolean, operandDimensions: (Int, Int), fixedPoint: Int) {
    require(wOperands > 0, "wOperands must be greater than 0")
    require(wResult >= wOperands, "wResult must be greater than or equal wOperands")
    require(operandDimensions._1 > 0, "operandDimensions._1 must be greater than 0")
    require(operandDimensions._2 > 0, "operandDimensions._2 must be greater than 0")
    require(fixedPoint >= 0, "fixedPoint must be greater than or equal 0")
  }
}
