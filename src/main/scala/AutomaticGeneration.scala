import activation_functions.ReLU
import chisel3._
import onnx.Operators._
import systolic_array.MatMul

class AutomaticGeneration(
                           listOfNodes: List[Any],
                           connectionList: List[List[Int]],
                           enableDebuggingIO: Boolean = true
                         ) extends Module {


  val modules = listOfNodes.map {
    case inputType: InputType =>
      val input = Module(new InputModule(inputType))
      input
    case outputType: OutputType =>
      val output = Module(new OutputModule(outputType))
      output
    case initializerType: InitializerType =>
      val initializer = Module(new Initializer(initializerType))
      initializer
    case addType: AddType =>
      val add = Module(new Add(addType, enableDebuggingIO))
      add
    case matMulType: MatMulType =>
      val matMul = Module(new MatMul(matMulType, enableDebuggingIO))
      matMul
    case reluType: ReLUType =>
      val relu = Module(new ReLU(reluType))
      relu
  }

  // TODO: Handle different widths by introducing rounders

  val inputNode = listOfNodes.head.asInstanceOf[InputType]
  val outputNode = listOfNodes.last.asInstanceOf[OutputType]
  val io = IO(new Bundle {
    // initializers
    val ready = Input(Bool()) // indicates that the producer has new data to be processed
    val input = Input(Vec(inputNode.dimensions._1, Vec(inputNode.dimensions._2, UInt(inputNode.w.W))))


    val output = Output(Vec(outputNode.dimensions._1, Vec(outputNode.dimensions._2, UInt(outputNode.w.W))))
    val valid = Output(Bool()) // indicates that the module should be done
  })

  for (i <- 0 until listOfNodes.length) {
    val currentModule = modules(i)
    val connections = connectionList(i)
    currentModule match {
      case input: InputModule =>
        input.io.ready := io.ready
        input.io.inputs := io.input
      case add: Add =>
        val connectedModule1 = modules(connections(0))
        val connectedModule2 = modules(connections(1))
        val ready1 = Wire(Bool())
        val ready2 = Wire(Bool())
        connectedModule1 match {
          case input: InputModule =>
            ready1 := input.io.valid
            add.io.input := input.io.outputs
          case add: Add =>
            ready1 := add.io.valid
            add.io.input := add.io.result
          case matMul: MatMul =>
            ready1 := matMul.io.valid
            add.io.input := matMul.io.result
          case relu: ReLU =>
            ready1 := relu.io.valid
            add.io.input := relu.io.result
          case initializer: Initializer =>
            ready1 := initializer.io.valid
            add.io.input := initializer.io.output
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to an add module")
          case _ =>
            throw new Exception("Unknown module connected to add module inputs")
        }
        connectedModule2 match {
          case input: InputModule =>
            ready2 := input.io.valid
            add.io.biases := input.io.outputs
          case add: Add =>
            ready2 := add.io.valid
            add.io.biases := add.io.result
          case matMul: MatMul =>
            ready2 := matMul.io.valid
            add.io.biases := matMul.io.result
          case relu: ReLU =>
            ready2 := relu.io.valid
            add.io.biases := relu.io.result
          case initializer: Initializer =>
            ready2 := initializer.io.valid
            add.io.biases := initializer.io.output
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to an add module")
          case _ =>
            throw new Exception("Unknown module connected to add module biases")
        }
        add.io.ready := ready1 && ready2
      case matMul: MatMul =>
        val connectedModule1 = modules(connections(0))
        val connectedModule2 = modules(connections(1))
        val ready1 = Wire(Bool())
        val ready2 = Wire(Bool())
        connectedModule1 match {
          case input: InputModule =>
            ready1 := input.io.valid
            matMul.io.inputs := input.io.outputs
          case add: Add =>
            ready1 := add.io.valid
            matMul.io.inputs := add.io.result
          case matMul: MatMul =>
            ready1 := matMul.io.valid
            matMul.io.inputs := matMul.io.result
          case relu: ReLU =>
            ready1 := relu.io.valid
            matMul.io.inputs := relu.io.result
          case initializer: Initializer =>
            ready1 := initializer.io.valid
            matMul.io.inputs := initializer.io.output
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a matmul module")
          case _ =>
            throw new Exception("Unknown module connected to matmul module inputs")
        }
        connectedModule2 match {
          case input: InputModule =>
            ready2 := input.io.valid
            matMul.io.weights := input.io.outputs
          case add: Add =>
            ready2 := add.io.valid
            matMul.io.weights := add.io.result
          case matMul: MatMul =>
            ready2 := matMul.io.valid
            matMul.io.weights := matMul.io.result
          case relu: ReLU =>
            ready2 := relu.io.valid
            matMul.io.weights := relu.io.result
          case initializer: Initializer =>
            ready2 := initializer.io.valid
            matMul.io.weights := initializer.io.output
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a matmul module")
          case _ =>
            throw new Exception("Unknown module connected to matmul module weights")
        }
        matMul.io.ready := ready1 && ready2

      case relu: ReLU =>
        val connectedModule = modules(connections(0))
        connectedModule match {
          case input: InputModule =>
            relu.io.input := input.io.outputs
            relu.io.ready := input.io.valid
          case add: Add =>
            relu.io.input := add.io.result
            relu.io.ready := add.io.valid
          case matMul: MatMul =>
            relu.io.input := matMul.io.result
            relu.io.ready := matMul.io.valid
          case relu: ReLU =>
            relu.io.input := relu.io.result
            relu.io.ready := relu.io.valid
          case initializer: Initializer =>
            relu.io.input := initializer.io.output
            relu.io.ready := initializer.io.valid
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a relu module")
          case _ =>
            throw new Exception("Unknown module connected to relu module")
        }

      case _: Initializer =>

      case output: OutputModule =>
        val connectedModule = modules(connections(0))
        connectedModule match {
          case input: InputModule =>
            output.io.inputs := input.io.outputs
            output.io.ready := input.io.valid
          case add: Add =>
            output.io.inputs := add.io.result
            output.io.ready := add.io.valid
          case matMul: MatMul =>
            output.io.inputs := matMul.io.result
            output.io.ready := matMul.io.valid
          case relu: ReLU =>
            output.io.inputs := relu.io.result
            output.io.ready := relu.io.valid
          case initializer: Initializer =>
            output.io.inputs := initializer.io.output
            output.io.ready := initializer.io.valid
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to an output module")
          case _ =>
            throw new Exception("Unknown module connected to output module")
        }
        io.output := output.io.outputs
        io.valid := output.io.valid
      case _ =>
        throw new Exception("Unknown module type")
    }
  }
}
