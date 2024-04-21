package onnx

import stages.{ConvImplementation, InputImplementation, MatMulImplementation, OutputImplementation}

object Operators {
  case class Parameters(signed: Boolean, w: Int, wResult: Int, fixedPoint: Int, fixedPointResult: Int) {
    require(w > 0, "w must be greater than 0")
    require(wResult >= 2 * w, "wResult must be greater than or equal w")
    require(fixedPoint >= 0, "fixedPoint must be greater than or equal 0")
    require(fixedPointResult >= 0, "fixedPointResult must be greater than or equal 0")
  }

  case class InputType(wIn: Int, wOut: Int, inputShape: (Int, Int, Int, Int), outputShape: (Int, Int, Int, Int), implementation: InputImplementation, frequency: Int, baudRate: Int) {
    require(wIn > 0, "w must be greater than 0")
    require(wOut > 0, "w must be greater than 0")
    require(inputShape._1 > 0, "dimensions._1 must be greater than 0")
    require(inputShape._2 > 0, "dimensions._2 must be greater than 0")
    require(inputShape._3 > 0, "dimensions._3 must be greater than 0")
    require(inputShape._4 > 0, "dimensions._4 must be greater than 0")
    require(outputShape._1 > 0, "dimensions._1 must be greater than 0")
    require(outputShape._2 > 0, "dimensions._2 must be greater than 0")
    require(outputShape._3 > 0, "dimensions._3 must be greater than 0")
    require(outputShape._4 > 0, "dimensions._4 must be greater than 0")
    require(frequency > 0, "frequency must be greater than 0")
    require(baudRate > 0, "baudRate must be greater than 0")
  }

  case class OutputType(wIn: Int, wOut: Int, inputShape: (Int, Int, Int, Int), outputShape: (Int, Int, Int, Int), implementation: OutputImplementation, frequency: Int, baudRate: Int) {
    require(wIn > 0, "w must be greater than 0")
    require(wOut > 0, "w must be greater than 0")
    require(inputShape._1 > 0, "dimensions._1 must be greater than 0")
    require(inputShape._2 > 0, "dimensions._2 must be greater than 0")
    require(inputShape._3 > 0, "dimensions._3 must be greater than 0")
    require(inputShape._4 > 0, "dimensions._4 must be greater than 0")
    require(outputShape._1 > 0, "dimensions._1 must be greater than 0")
    require(outputShape._2 > 0, "dimensions._2 must be greater than 0")
    require(outputShape._3 > 0, "dimensions._3 must be greater than 0")
    require(outputShape._4 > 0, "dimensions._4 must be greater than 0")
    require(frequency > 0, "frequency must be greater than 0")
    require(baudRate > 0, "baudRate must be greater than 0")
  }

  case class RounderType(wOperands: Int, wResult: Int, signed: Boolean, shape: (Int, Int, Int, Int), fixedPoint: Int) {
    require(wOperands > 0, "wOperands must be greater than 0")
    require(wResult <= wOperands, "wResult must be greater than or equal wOperands")
    require(shape._1 > 0, "dimensions._1 must be greater than 0")
    require(shape._2 > 0, "dimensions._2 must be greater than 0")
    require(shape._3 > 0, "dimensions._3 must be greater than 0")
    require(shape._4 > 0, "dimensions._4 must be greater than 0")
    require(fixedPoint >= 0, "fixedPoint must be greater than or equal 0")
  }

  case class ConvType(w: Int, wResult: Int, inputShape: (Int, Int, Int, Int), kernelShape: (Int, Int, Int, Int), signed: Boolean, strides: (Int, Int), pads: (Int, Int), implementation: ConvImplementation) {
    require(w > 0, "w must be greater than 0")
    require(wResult >= w, "wResult must be greater than or equal w")
    require(inputShape._1 > 0, "inputDimensions._1 must be greater than 0")
    require(inputShape._2 > 0, "inputDimensions._2 must be greater than 0")
    require(kernelShape._1 > 0, "kernelDimensions._1 must be greater than 0")
    require(kernelShape._2 > 0, "kernelDimensions._2 must be greater than 0")
    require(strides._1 > 0, "strides._1 must be greater than 0")
    require(strides._2 > 0, "strides._2 must be greater than 0")
    require(pads._1 >= 0, "pads._1 must be greater than or equal 0")
    require(pads._2 >= 0, "pads._2 must be greater than or equal 0")
  }

  case class MatMulType(wOperands: Int, wResult: Int, signed: Boolean, operandAShape: (Int, Int, Int, Int), operandBShape: (Int, Int, Int, Int), implementation: MatMulImplementation) {
    require(wOperands > 0, "wOperands must be greater than 0")
    require(wResult >= 2 * wOperands, "wResult must be greater than or equal 2 * wOperands")
    require(operandAShape._1 > 0, "operandADimensions._1 must be greater than 0")
    require(operandAShape._2 > 0, "operandADimensions._2 must be greater than 0")
    require(operandAShape._3 > 0, "operandADimensions._3 must be greater than 0")
    require(operandAShape._4 > 0, "operandADimensions._4 must be greater than 0")
    require(operandBShape._1 > 0, "operandBDimensions._1 must be greater than 0")
    require(operandBShape._2 > 0, "operandBDimensions._2 must be greater than 0")
    require(operandBShape._3 > 0, "operandBDimensions._3 must be greater than 0")
    require(operandBShape._4 > 0, "operandBDimensions._4 must be greater than 0")
  }

  case class MaxPoolType(w: Int, inputShape: (Int, Int, Int, Int), signed: Boolean, kernelShape: (Int, Int), strides: (Int, Int), pads: (Int, Int)) {
    require(w > 0, "w must be greater than 0")
    require(inputShape._1 > 0, "inputDimensions._1 must be greater than 0")
    require(inputShape._2 > 0, "inputDimensions._2 must be greater than 0")
    require(inputShape._3 > 0, "inputDimensions._3 must be greater than 0")
    require(inputShape._4 > 0, "inputDimensions._4 must be greater than 0")
    require(kernelShape._1 > 0, "kernelDimensions._1 must be greater than 0")
    require(kernelShape._2 > 0, "kernelDimensions._2 must be greater than 0")
    require(strides._1 > 0, "strides._1 must be greater than 0")
    require(strides._2 > 0, "strides._2 must be greater than 0")
    require(pads._1 >= 0, "pads._1 must be greater than or equal 0")
    require(pads._2 >= 0, "pads._2 must be greater than or equal 0")
  }

  case class ReshapeType(w: Int, inputShape: (Int, Int, Int, Int), shapeShape: (Int, Int, Int, Int), newShape: (Int, Int, Int, Int)) {
    require(w > 0, "w must be greater than 0")
    require(inputShape._1 > 0, "inputDimensions._1 must be greater than 0")
    require(inputShape._2 > 0, "inputDimensions._2 must be greater than 0")
    require(inputShape._3 > 0, "inputDimensions._3 must be greater than 0")
    require(inputShape._4 > 0, "inputDimensions._4 must be greater than 0")
    require(shapeShape._1 > 0, "shapeDimensions._1 must be greater than 0")
    require(shapeShape._2 > 0, "shapeDimensions._2 must be greater than 0")
    require(shapeShape._3 > 0, "shapeDimensions._3 must be greater than 0")
    require(shapeShape._4 > 0, "shapeDimensions._4 must be greater than 0")
    require(newShape._1 > 0, "newDimensions._1 must be greater than 0")
    require(newShape._2 > 0, "newDimensions._2 must be greater than 0")
    require(newShape._3 > 0, "newDimensions._3 must be greater than 0")
    require(newShape._4 > 0, "newDimensions._4 must be greater than 0")
  }

  case class ReluType(w: Int, signed: Boolean, shape: (Int, Int, Int, Int)) {
    require(w > 0, "wOperands must be greater than 0")
    require(shape._1 > 0, "operandDimensions._1 must be greater than 0")
    require(shape._2 > 0, "operandDimensions._2 must be greater than 0")
    require(shape._3 > 0, "operandDimensions._3 must be greater than 0")
    require(shape._4 > 0, "operandDimensions._4 must be greater than 0")
  }

  case class AddType(w: Int, shape: (Int, Int, Int, Int)) {
    require(w > 0, "wOperands must be greater than 0")
    require(shape._1 > 0, "operandDimensions._1 must be greater than 0")
    require(shape._2 > 0, "operandDimensions._2 must be greater than 0")
    require(shape._3 > 0, "operandDimensions._3 must be greater than 0")
    require(shape._4 > 0, "operandDimensions._4 must be greater than 0")
  }

  case class InitializerType(w: Int, shape: (Int, Int, Int, Int), data: Array[Array[Array[Array[BigInt]]]]) {
    require(w >= 0, "w must be greater or equal 0 (0 is a special case)")
    require(shape._1 > 0, "dimensions._1 must be greater than 0")
    require(shape._2 > 0, "dimensions._2 must be greater than 0")
    require(shape._3 > 0, "dimensions._3 must be greater than 0")
    require(shape._4 > 0, "dimensions._4 must be greater than 0")
  }

  case class BroadcasterType(w: Int, operandShape: (Int, Int, Int, Int), newShape: (Int, Int, Int, Int)) {
    require(w > 0, "w must be greater than 0")
    require(operandShape._1 > 0, "operandDimensions._1 must be greater than 0")
    require(operandShape._2 > 0, "operandDimensions._2 must be greater than 0")
    require(operandShape._3 > 0, "operandDimensions._3 must be greater than 0")
    require(operandShape._4 > 0, "operandDimensions._4 must be greater than 0")
    require(newShape._1 > 0, "newDimensions._1 must be greater than 0")
    require(newShape._2 > 0, "newDimensions._2 must be greater than 0")
    require(newShape._3 > 0, "newDimensions._3 must be greater than 0")
    require(newShape._4 > 0, "newDimensions._4 must be greater than 0")
  }
}
