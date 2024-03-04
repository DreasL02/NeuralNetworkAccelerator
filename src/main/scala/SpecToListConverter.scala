import onnx.Operators

object SpecToListConverter {

  // Reads a spec file (e.g. "scala_utils/data/example_spec_file.json") and converts it to various lists of the ONNX types
  def convertSpecToLists(specFilePath: String): (List[Any], List[List[Int]]) = {
    val spec = scala.io.Source.fromFile(specFilePath).mkString

    val json = ujson.read(spec)
    val inputs = json("Input").arr
    val outputs = json("Output").arr
    val initializers = json("Initializer").arr
    val add = json("Add").arr
    val matmul = json("MatMul").arr
    val relu = json("ReLU").arr
    val round = json("Round").arr
    val inputList = inputs.map(input => {
      val w = input("bit_width").num.toInt
      val dimensions = (input("input_dims")(0)(0).num.toInt, input("input_dims")(0)(1).num.toInt)
      val index = input("index").num.toInt
      (index, Operators.InputType(w, dimensions), List())
    }).toList

    val outputList = outputs.map(output => {
      val w = output("bit_width").num.toInt
      val dimensions = (output("input_dims")(0)(0).num.toInt, output("input_dims")(0)(1).num.toInt)
      val index = output("index").num.toInt
      val connectionIndex = output("connections")(0).num.toInt
      (index, Operators.OutputType(w, dimensions), List(connectionIndex))
    }).toList

    val initializerList = initializers.map(initializer => {
      val dimensions = (initializer("input_dims")(0)(0).num.toInt, initializer("input_dims")(0)(1).num.toInt)
      val w = initializer("bit_width").num.toInt
      val index = initializer("index").num.toInt
      // TODO: Find out how data is best represented in the JSON file / in another file

      (index, Operators.InitializerType(dimensions, w, null), List())
    }).toList

    val addList = add.map(add => {
      val wOperands = add("bit_width").num.toInt
      val operandDimensions = (add("input_dims")(0)(0).num.toInt, add("input_dims")(0)(1).num.toInt)
      val index = add("index").num.toInt
      val connection1 = add("connections")(0).num.toInt
      val connection2 = add("connections")(1).num.toInt
      (index, Operators.AddType(wOperands, operandDimensions), List(connection1, connection2))
    }).toList

    val matmulList = matmul.map(matmul => {
      val wOperands = matmul("bit_width_operands").num.toInt
      val wResult = matmul("bit_width_result").num.toInt
      val signed = matmul("signed").bool
      val operandADim = (matmul("input_dims")(0)(0).num.toInt, matmul("input_dims")(0)(1).num.toInt)
      val operandBDim = (matmul("input_dims")(1)(0).num.toInt, matmul("input_dims")(1)(1).num.toInt)
      val index = matmul("index").num.toInt
      val connection1 = matmul("connections")(0).num.toInt
      val connection2 = matmul("connections")(1).num.toInt
      (index, Operators.MatMulType(wOperands, wResult, signed, operandADim, operandBDim), List(connection1, connection2))
    }).toList

    val reluList = relu.map(relu => {
      val wOperands = relu("bit_width").num.toInt
      val signed = relu("signed").bool
      val operandDimensions = (relu("input_dims")(0)(0).num.toInt, relu("input_dims")(0)(1).num.toInt)
      val index = relu("index").num.toInt
      val connectionIndex = relu("connections")(0).num.toInt
      (index, Operators.ReLUType(wOperands, signed, operandDimensions), List(connectionIndex))
    }).toList

    val roundList = round.map(round => {
      val wOperands = round("bit_width_operands").num.toInt
      val wResult = round("bit_width_result").num.toInt
      val signed = round("signed").bool
      val operandDimensions = (round("input_dims")(0)(0).num.toInt, round("input_dims")(0)(1).num.toInt)
      val fixedPoint = round("fixed_point_position").num.toInt
      val index = round("index").num.toInt
      val connectionIndex = round("connections")(0).num.toInt
      (index, Operators.RoundType(wOperands, wResult, signed, operandDimensions, fixedPoint), List(connectionIndex))
    }).toList

    // only one input and one output is supported
    if (inputList.length != 1 || outputList.length != 1) {
      throw new Exception("Only one input and one output is supported")
    }

    val allLists = List(inputList, outputList, initializerList, addList, matmulList, reluList, roundList)
    val sortedList = allLists.flatten.sortBy(_._1).map(_._2)
    val connectionList = allLists.flatten.sortBy(_._1).map(_._3)
    (sortedList, connectionList)
  }
}
