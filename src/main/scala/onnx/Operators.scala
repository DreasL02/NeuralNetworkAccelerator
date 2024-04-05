package onnx

object Operators {
  case class Parameters(signed: Boolean, w: Int, wResult: Int, fixedPoint: Int, fixedPointResult: Int) {
    require(w > 0, "w must be greater than 0")
    require(wResult >= 2 * w, "wResult must be greater than or equal w")
    require(fixedPoint >= 0, "fixedPoint must be greater than or equal 0")
    require(fixedPointResult >= 0, "fixedPointResult must be greater than or equal 0")
  }

  case class InputType(w: Int, dimensions: (Int, Int, Int, Int)) {
    require(w > 0, "w must be greater than 0")
    require(dimensions._1 > 0, "dimensions._1 must be greater than 0")
    require(dimensions._2 > 0, "dimensions._2 must be greater than 0")
    require(dimensions._3 > 0, "dimensions._3 must be greater than 0")
    require(dimensions._4 > 0, "dimensions._4 must be greater than 0")
  }

  case class OutputType(w: Int, dimensions: (Int, Int, Int, Int)) {
    require(w > 0, "w must be greater than 0")
    require(dimensions._1 > 0, "dimensions._1 must be greater than 0")
    require(dimensions._2 > 0, "dimensions._2 must be greater than 0")
    require(dimensions._3 > 0, "dimensions._3 must be greater than 0")
    require(dimensions._4 > 0, "dimensions._4 must be greater than 0")
  }

  case class RounderType(wOperands: Int, wResult: Int, signed: Boolean, dimensions: (Int, Int, Int, Int), fixedPoint: Int) {
    require(wOperands > 0, "wOperands must be greater than 0")
    require(wResult <= wOperands, "wResult must be greater than or equal wOperands")
    require(dimensions._1 > 0, "dimensions._1 must be greater than 0")
    require(dimensions._2 > 0, "dimensions._2 must be greater than 0")
    require(dimensions._3 > 0, "dimensions._3 must be greater than 0")
    require(dimensions._4 > 0, "dimensions._4 must be greater than 0")
    require(fixedPoint >= 0, "fixedPoint must be greater than or equal 0")
  }

  case class ConvType(w: Int, wResult: Int, inputDimensions: (Int, Int, Int, Int), kernelDimensions: (Int, Int, Int, Int), signed: Boolean, strides: (Int, Int), pads: (Int, Int)) {
    require(w > 0, "w must be greater than 0")
    require(wResult >= w, "wResult must be greater than or equal w")
    require(inputDimensions._1 > 0, "inputDimensions._1 must be greater than 0")
    require(inputDimensions._2 > 0, "inputDimensions._2 must be greater than 0")
    require(kernelDimensions._1 > 0, "kernelDimensions._1 must be greater than 0")
    require(kernelDimensions._2 > 0, "kernelDimensions._2 must be greater than 0")
    require(strides._1 > 0, "strides._1 must be greater than 0")
    require(strides._2 > 0, "strides._2 must be greater than 0")
    require(pads._1 >= 0, "pads._1 must be greater than or equal 0")
    require(pads._2 >= 0, "pads._2 must be greater than or equal 0")
  }

  case class MatMulType(wOperands: Int, wResult: Int, signed: Boolean, operandADimensions: (Int, Int, Int, Int), operandBDimensions: (Int, Int, Int, Int)) {
    require(wOperands > 0, "wOperands must be greater than 0")
    require(wResult >= 2 * wOperands, "wResult must be greater than or equal 2 * wOperands")
    require(operandADimensions._1 > 0, "operandADimensions._1 must be greater than 0")
    require(operandADimensions._2 > 0, "operandADimensions._2 must be greater than 0")
    require(operandADimensions._3 > 0, "operandADimensions._3 must be greater than 0")
    require(operandADimensions._4 > 0, "operandADimensions._4 must be greater than 0")
    require(operandBDimensions._1 > 0, "operandBDimensions._1 must be greater than 0")
    require(operandBDimensions._2 > 0, "operandBDimensions._2 must be greater than 0")
    require(operandBDimensions._3 > 0, "operandBDimensions._3 must be greater than 0")
    require(operandBDimensions._4 > 0, "operandBDimensions._4 must be greater than 0")
  }

  case class MaxPoolType(w: Int, inputDimensions: (Int, Int, Int, Int), signed: Boolean, kernelDimensions: (Int, Int), strides: (Int, Int), pads: (Int, Int)) {
    require(w > 0, "w must be greater than 0")
    require(inputDimensions._1 > 0, "inputDimensions._1 must be greater than 0")
    require(inputDimensions._2 > 0, "inputDimensions._2 must be greater than 0")
    require(inputDimensions._3 > 0, "inputDimensions._3 must be greater than 0")
    require(inputDimensions._4 > 0, "inputDimensions._4 must be greater than 0")
    require(kernelDimensions._1 > 0, "kernelDimensions._1 must be greater than 0")
    require(kernelDimensions._2 > 0, "kernelDimensions._2 must be greater than 0")
    require(strides._1 > 0, "strides._1 must be greater than 0")
    require(strides._2 > 0, "strides._2 must be greater than 0")
    require(pads._1 >= 0, "pads._1 must be greater than or equal 0")
    require(pads._2 >= 0, "pads._2 must be greater than or equal 0")
  }

  case class ReshapeType(w: Int, inputDimensions: (Int, Int, Int, Int), shapeDimensions: (Int, Int, Int, Int), newDimensions: (Int, Int, Int, Int)) {
    require(w > 0, "w must be greater than 0")
    require(inputDimensions._1 > 0, "inputDimensions._1 must be greater than 0")
    require(inputDimensions._2 > 0, "inputDimensions._2 must be greater than 0")
    require(inputDimensions._3 > 0, "inputDimensions._3 must be greater than 0")
    require(inputDimensions._4 > 0, "inputDimensions._4 must be greater than 0")
    require(shapeDimensions._1 > 0, "shapeDimensions._1 must be greater than 0")
    require(shapeDimensions._2 > 0, "shapeDimensions._2 must be greater than 0")
    require(shapeDimensions._3 > 0, "shapeDimensions._3 must be greater than 0")
    require(shapeDimensions._4 > 0, "shapeDimensions._4 must be greater than 0")
    require(newDimensions._1 > 0, "newDimensions._1 must be greater than 0")
    require(newDimensions._2 > 0, "newDimensions._2 must be greater than 0")
    require(newDimensions._3 > 0, "newDimensions._3 must be greater than 0")
    require(newDimensions._4 > 0, "newDimensions._4 must be greater than 0")
  }

  case class ReluType(w: Int, signed: Boolean, dimensions: (Int, Int, Int, Int)) {
    require(w > 0, "wOperands must be greater than 0")
    require(dimensions._1 > 0, "operandDimensions._1 must be greater than 0")
    require(dimensions._2 > 0, "operandDimensions._2 must be greater than 0")
    require(dimensions._3 > 0, "operandDimensions._3 must be greater than 0")
    require(dimensions._4 > 0, "operandDimensions._4 must be greater than 0")
  }

  case class AddType(w: Int, operandDimensions: (Int, Int, Int, Int)) {
    require(w > 0, "wOperands must be greater than 0")
    require(operandDimensions._1 > 0, "operandDimensions._1 must be greater than 0")
    require(operandDimensions._2 > 0, "operandDimensions._2 must be greater than 0")
    require(operandDimensions._3 > 0, "operandDimensions._3 must be greater than 0")
    require(operandDimensions._4 > 0, "operandDimensions._4 must be greater than 0")
  }

  case class InitializerType(w: Int, dimensions: (Int, Int, Int, Int), data: Array[Array[Array[Array[BigInt]]]]) {
    require(w >= 0, "w must be greater or equal 0 (0 is a special case)")
    require(dimensions._1 > 0, "dimensions._1 must be greater than 0")
    require(dimensions._2 > 0, "dimensions._2 must be greater than 0")
    require(dimensions._3 > 0, "dimensions._3 must be greater than 0")
    require(dimensions._4 > 0, "dimensions._4 must be greater than 0")
  }

  case class BroadcasterType(w: Int, operandDimensions: (Int, Int, Int, Int), newDimensions: (Int, Int, Int, Int)) {
    require(w > 0, "w must be greater than 0")
    require(operandDimensions._1 > 0, "operandDimensions._1 must be greater than 0")
    require(operandDimensions._2 > 0, "operandDimensions._2 must be greater than 0")
    require(operandDimensions._3 > 0, "operandDimensions._3 must be greater than 0")
    require(operandDimensions._4 > 0, "operandDimensions._4 must be greater than 0")
    require(newDimensions._1 > 0, "newDimensions._1 must be greater than 0")
    require(newDimensions._2 > 0, "newDimensions._2 must be greater than 0")
    require(newDimensions._3 > 0, "newDimensions._3 must be greater than 0")
    require(newDimensions._4 > 0, "newDimensions._4 must be greater than 0")
  }
}
