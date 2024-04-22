package onnx

import scala_utils.MatrixUtils.tensorToString
import stages.{ConvImplementation, InputImplementation, MatMulImplementation, OutputImplementation}

import scala.math.BigDecimal.double2bigDecimal

object SpecToListConverter {
  private def toTuple2(array: Array[Int]): (Int, Int) = {
    require(array.length == 2)
    (array(0), array(1))
  }

  private def toTuple4(array: Array[Int]): (Int, Int, Int, Int) = {
    require(array.length == 4)
    (array(0), array(1), array(2), array(3))
  }

  // Reads a spec file (e.g. "scala_utils/data/example_spec_file.json") and converts it to various lists of the ONNX types
  def convertSpecToLists(specFilePath: String, toPrint: Boolean = false): (Operators.Parameters, List[Any], List[List[Int]]) = {
    val source = scala.io.Source.fromFile(specFilePath)
    val spec = try source.mkString finally source.close()

    val json = ujson.read(spec)
    val inputs = json("Input").arr
    val outputs = json("Output").arr
    val rounders = json("Rounder").arr
    val convs = json("Conv").arr
    val matmuls = json("MatMul").arr
    val maxPools = json("MaxPool").arr
    val reshapes = json("Reshape").arr
    val relus = json("Relu").arr
    val adds = json("Add").arr
    val initializers = json("Initializer").arr
    val broadcasters = json("Broadcaster").arr
    val modelParameters = json("Parameters").arr

    val parameters = modelParameters.map(entry => {
      val signed = entry("signed").bool
      val w = entry("bit_width_multiplication").num.toInt
      val wResult = entry("bit_width_base").num.toInt
      val fixedPoint = entry("fixed_point_multiplication").num.toInt
      val fixedPointResult = entry("fixed_point_base").num.toInt
      Operators.Parameters(signed, w, wResult, fixedPoint, fixedPointResult)
    }).toList

    if (parameters.length != 1) {
      throw new Exception("Only one set of model parameters is supported")
    }

    if (toPrint) println("Name | Index | Bit Width | Shape | Connection Index (data is shown below the list)")

    val inputList = inputs.map(entry => {
      val w = entry("bit_width").num.toInt
      val shape = toTuple4(entry("shape").arr.map(_.num.toInt).toArray)
      val index = entry("index").num.toInt
      val implementationString = entry("implementation").str
      val implementation = implementationString match {
        case "open" => InputImplementation.Open
        case "uart" => InputImplementation.Uart
        case _ => throw new Exception("Unknown implementation: " + implementationString)
      }
      val frequency = entry("frequency").num.toInt
      val baudRate = entry("baud_rate").num.toInt
      val inputShape = toTuple4(entry("input_shape").arr.map(_.num.toInt).toArray)
      val inputW = entry("input_bit_width").num.toInt
      if (toPrint) println("Input: " + index + " " + w + " " + shape)
      (index, Operators.InputType(inputW, w, inputShape, shape, implementation, frequency, baudRate), List())
    }).toList

    val outputList = outputs.map(entry => {
      val w = entry("bit_width").num.toInt
      val shape = toTuple4(entry("shape").arr.map(_.num.toInt).toArray)
      val index = entry("index").num.toInt
      val connectionIndex = entry("connections").arr.map(_.num.toInt).toList
      val implementationString = entry("implementation").str
      val implementation = implementationString match {
        case "open" => OutputImplementation.Open
        case "uart" => OutputImplementation.Uart
        case _ => throw new Exception("Unknown implementation: " + implementationString)
      }
      val frequency = entry("frequency").num.toInt
      val baudRate = entry("baud_rate").num.toInt
      val inputShape = toTuple4(entry("input_shape").arr.map(_.num.toInt).toArray)
      val inputW = entry("input_bit_width").num.toInt
      if (toPrint) println("Output: " + index + " " + w + " " + shape + " " + connectionIndex)
      (index, Operators.OutputType(inputW, w, inputShape, shape, implementation, frequency, baudRate), connectionIndex)
    }).toList

    val rounderList = rounders.map(entry => {
      val wOperands = entry("bit_width_operands").num.toInt
      val wResult = entry("bit_width_result").num.toInt
      val signed = parameters(0).signed
      val operandShape = toTuple4(entry("input_shape")(0).arr.map(_.num.toInt).toArray)
      val fixedPoint = entry("fixed_point_result").num.toInt
      val index = entry("index").num.toInt
      val connectionIndex = entry("connections").arr.map(_.num.toInt).toList
      if (toPrint) println("Rounder: " + index + " " + wOperands + " " + wResult + " " + signed + " " + operandShape + " " + fixedPoint + " " + connectionIndex)
      (index, Operators.RounderType(wOperands, wResult, signed, operandShape, fixedPoint), connectionIndex)
    }).toList

    val convList = convs.map(entry => {
      val w = entry("bit_width_operands").num.toInt
      val wResult = entry("bit_width_result").num.toInt
      val inputShape = toTuple4(entry("input_shape")(0).arr.map(_.num.toInt).toArray)
      val kernelShape = toTuple4(entry("input_shape")(1).arr.map(_.num.toInt).toArray)
      val signed = parameters(0).signed
      val strides = toTuple2(entry("strides").arr.map(_.num.toInt).toArray)
      val pads = toTuple2(entry("padding").arr.map(_.num.toInt).toArray)
      val index = entry("index").num.toInt
      val connectionIndex = entry("connections").arr.map(_.num.toInt).toList
      val implementationString = entry("implementation").str
      val implementation = implementationString match {
        case "direct" => ConvImplementation.Direct
        case "im2col" => ConvImplementation.Im2Col
        case _ => throw new Exception("Unknown implementation: " + implementationString)
      }
      if (toPrint) println("Conv: " + index + " " + w + " " + wResult + " " + inputShape + " " + kernelShape + " " + signed + " " + strides + " " + pads + " " + connectionIndex)
      (index, Operators.ConvType(w, wResult, inputShape, kernelShape, signed, strides, pads, implementation), connectionIndex)
    }).toList

    val matmulList = matmuls.map(entry => {
      val wOperands = entry("bit_width_operands").num.toInt
      val wResult = entry("bit_width_result").num.toInt
      val signed = parameters(0).signed
      val operandAShape = toTuple4(entry("input_shape")(0).arr.map(_.num.toInt).toArray)
      val operandBShape = toTuple4(entry("input_shape")(1).arr.map(_.num.toInt).toArray)
      val index = entry("index").num.toInt
      val connectionIndex = entry("connections").arr.map(_.num.toInt).toList
      val implementationString = entry("implementation").str
      val implementation = implementationString match {
        case "direct" => MatMulImplementation.Direct
        case "systolic_array" => MatMulImplementation.SystolicArray
        case _ => throw new Exception("Unknown implementation: " + implementationString)
      }
      if (toPrint) println("MatMul: " + index + " " + wOperands + " " + wResult + " " + signed + " " + operandAShape + " " + operandBShape + " " + connectionIndex)
      (index, Operators.MatMulType(wOperands, wResult, signed, operandAShape, operandBShape, implementation), connectionIndex)
    }).toList

    val maxPoolList = maxPools.map(entry => {
      val w = entry("bit_width").num.toInt
      val inputShape = toTuple4(entry("input_shape")(0).arr.map(_.num.toInt).toArray)
      val kernelShape = toTuple2(entry("kernel_shape").arr.map(_.num.toInt).toArray)
      val signed = parameters(0).signed
      val strides = toTuple2(entry("strides").arr.map(_.num.toInt).toArray)
      val pads = toTuple2(entry("padding").arr.map(_.num.toInt).toArray)
      val index = entry("index").num.toInt
      val connectionIndex = entry("connections").arr.map(_.num.toInt).toList
      if (toPrint) println("MaxPool: " + index + " " + w + " " + inputShape + " " + signed + " " + kernelShape + " " + strides + " " + pads + " " + connectionIndex)
      (index, Operators.MaxPoolType(w, inputShape, signed, kernelShape, strides, pads), connectionIndex)
    }).toList

    val reshapeList = reshapes.map(entry => {
      val w = entry("bit_width").num.toInt
      val inputShape = toTuple4(entry("input_shape")(0).arr.map(_.num.toInt).toArray)
      val shapeShape = toTuple4(entry("input_shape")(1).arr.map(_.num.toInt).toArray)
      val outputShape = toTuple4(entry("shape").arr.map(_.num.toInt).toArray)
      val index = entry("index").num.toInt
      val connectionIndex = entry("connections").arr.map(_.num.toInt).toList
      if (toPrint) println("Reshape: " + index + " " + w + " " + inputShape + " " + shapeShape + " " + outputShape + " " + connectionIndex)
      (index, Operators.ReshapeType(w, inputShape, shapeShape, outputShape), connectionIndex)
    }).toList

    val reluList = relus.map(entry => {
      val w = entry("bit_width").num.toInt
      val inputShape = toTuple4(entry("input_shape")(0).arr.map(_.num.toInt).toArray)
      val signed = parameters(0).signed
      val index = entry("index").num.toInt
      val connectionIndex = entry("connections").arr.map(_.num.toInt).toList
      if (toPrint) println("Relu: " + index + " " + w + " " + inputShape + " " + connectionIndex)
      (index, Operators.ReluType(w, signed, inputShape), connectionIndex)
    }).toList

    val addList = adds.map(entry => {
      val w = entry("bit_width").num.toInt
      val inputShape = toTuple4(entry("input_shape")(0).arr.map(_.num.toInt).toArray)
      val index = entry("index").num.toInt
      val connectionIndex = entry("connections").arr.map(_.num.toInt).toList
      if (toPrint) println("Add: " + index + " " + w + " " + inputShape + " " + connectionIndex)
      (index, Operators.AddType(w, inputShape), connectionIndex)
    }).toList

    val initializerList = initializers.map(entry => {
      val w = entry("bit_width").num.toInt
      val shape = toTuple4(entry("shape").arr.map(_.num.toInt).toArray)
      val index = entry("index").num.toInt
      val flat_data = entry("data").arr.map(_.num.toBigInt).toArray
      val data = flat_data.grouped(shape._1 * shape._2 * shape._3 * shape._4).toArray.map(
        _.grouped(shape._2 * shape._3 * shape._4).toArray.map(
          _.grouped(shape._3 * shape._4).toArray.map(
            _.grouped(shape._4).toArray))).head
      if (toPrint) println("Initializer: " + index + " " + w + " " + shape)
      if (toPrint) print(tensorToString(data))
      (index, Operators.InitializerType(w, shape, data), List())
    }).toList

    val broadcasterList = broadcasters.map(entry => {
      val w = entry("bit_width").num.toInt
      val inputShape = toTuple4(entry("input_shape")(0).arr.map(_.num.toInt).toArray)
      val outputShape = toTuple4(entry("shape").arr.map(_.num.toInt).toArray)
      val index = entry("index").num.toInt
      val connectionIndex = entry("connections").arr.map(_.num.toInt).toList
      if (toPrint) println("Broadcaster: " + index + " " + w + " " + inputShape + " " + outputShape + " " + connectionIndex)
      (index, Operators.BroadcasterType(w, inputShape, outputShape), connectionIndex)
    }).toList

    val allLists = List(inputList, outputList, rounderList, convList, matmulList, maxPoolList, reshapeList, reluList, addList, initializerList, broadcasterList)
    // sort elements by index (first element of tuple):
    val sortedList = allLists.flatten.sortBy(_._1)

    val modulesList = sortedList.map(_._2)
    val connectionList = sortedList.map(_._3)

    if (toPrint) {
      println("Sorted List: ")
      sortedList.foreach(println)
    }


    (parameters(0), modulesList, connectionList)
  }
}
