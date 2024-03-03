import onnx.Operators

class SpecToListConverter {

  // Reads a spec file (e.g. "scala_utils/data/example_spec_file.json") and converts it to various lists of the ONNX types
  def convertSpecToLists(specFilePath: String): List[Any] = {
    val spec = scala.io.Source.fromFile(specFilePath).mkString

    val json = ujson.read(spec)
    val inputs = json("Input").arr
    val outputs = json("Output").arr
    val initializers = json("Initializer").arr
    val add = json("Add").arr
    val matmul = json("MatMul").arr
    val relu = json("ReLU").arr

    val inputList = inputs.map(input => {
      val w = input("bit_width").num.toInt
      val dimensions = (input("input_dims")(0).num.toInt, input("input_dims")(1).num.toInt)
      val index = input("index").num.toInt
      (index, Operators.InputType(w, dimensions))
    }).toList

    val outputList = outputs.map(output => {
      val w = output("bit_width").num.toInt
      val dimensions = (output("input_dims")(0).num.toInt, output("input_dims")(1).num.toInt)
      val index = output("index").num.toInt
      val connectionIndex = output("connections").num.toInt
      (index, Operators.OutputType(w, dimensions, connectionIndex))
    }).toList

    val initializerList = initializers.map(initializer => {
      val dimensions = (initializer("input_dims")(0).num.toInt, initializer("input_dims")(1).num.toInt)
      val w = initializer("bit_width").num.toInt
      val index = initializer("index").num.toInt
      // TODO: Find out how data is best represented in the JSON file / in another file
      //val data = initializer("data").arr.map(row => row.arr.map(_.num.toInt).toSeq).toSeq
      (index, Operators.InitializerType(dimensions, w, null))
    }).toList

    val addList = add.map(add => {
      val wOperands = add("bit_width").num.toInt
      val operandDimensions = (add("input_dims")(0)(0).num.toInt, add("input_dims")(0)(1).num.toInt)
      val index = add("index").num.toInt
      val connectionIndex = (add("connections")(0).num.toInt, add("connections")(1).num.toInt)
      (index, Operators.AddType(wOperands, operandDimensions, connectionIndex))
    }).toList

    val matmulList = matmul.map(matmul => {
      val wOperands = matmul("bit_width_operands").num.toInt
      val wResult = matmul("bit_width_result").num.toInt
      val signed = true // TODO: make this a parameter in the JSON file
      val operandADim = (matmul("input_dims")(0)(0).num.toInt, matmul("input_dims")(0)(1).num.toInt)
      val operandBDim = (matmul("input_dims")(1)(0).num.toInt, matmul("input_dims")(1)(1).num.toInt)
      val index = matmul("index").num.toInt
      val connectionIndex = (matmul("connections")(0).num.toInt, matmul("connections")(1).num.toInt)
      (index, Operators.MatMulType(wOperands, wResult, signed, operandADim, operandBDim, connectionIndex))
    }).toList

    val reluList = relu.map(relu => {
      val wOperands = relu("bit_width").num.toInt
      val signed = true // TODO: make this a parameter in the JSON file
      val operandDimensions = (relu("input_dims")(0).num.toInt, relu("input_dims")(1).num.toInt)
      val index = relu("index").num.toInt
      val connectionIndex = relu("connections").num.toInt
      (index, Operators.ReLUType(wOperands, signed, operandDimensions, connectionIndex))
    }).toList

    val allLists = List(inputList, outputList, initializerList, addList, matmulList, reluList)
    val sortedList = allLists.flatten.sortBy(_._1).map(_._2)
    sortedList
  }
}
