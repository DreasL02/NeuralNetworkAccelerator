package onnx

object Operators {

  type InputType = {
    val w: Int
    val dimensions: (Int, Int)
  }

  type OutputType = {
    val w: Int
    val dimensions: (Int, Int)
  }

  type initializerType = {
    val dimensions: (Int, Int)
    val w: Int
    val data: Seq[Seq[Int]]
  }

  type AddType = {
    val wOperands: Int
    val fixedPoint: Int

    val operandDimensions: (Int, Int)
  }

  type MatMulType = {
    val wOperands: Int
    val wResult: Int
    val fixedPointOfOperands: Int
    val signed: Boolean

    val operandADimensions: (Int, Int)
    val operandBDimensions: (Int, Int)
  }

  type ReLUType = {
    val wOperands: Int
    val signed: Boolean

    val operandDimensions: (Int, Int)
  }
}
