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
          case conInput: InputModule =>
            ready1 := conInput.io.valid
            add.io.input := conInput.io.outputs
          case conAdd: Add =>
            ready1 := conAdd.io.valid
            add.io.input := conAdd.io.result
          case conMatMul: MatMul =>
            ready1 := conMatMul.io.valid
            add.io.input := conMatMul.io.result
          case conReLU: ReLU =>
            ready1 := conReLU.io.valid
            add.io.input := conReLU.io.result
          case conInitializer: Initializer =>
            ready1 := conInitializer.io.valid
            add.io.input := conInitializer.io.output
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to an add module")
          case _ =>
            throw new Exception("Unknown module connected to add module inputs")
        }
        connectedModule2 match {
          case conInput: InputModule =>
            ready2 := conInput.io.valid
            add.io.biases := conInput.io.outputs
          case conAdd: Add =>
            ready2 := conAdd.io.valid
            add.io.biases := conAdd.io.result
          case conMatMul: MatMul =>
            ready2 := conMatMul.io.valid
            add.io.biases := conMatMul.io.result
          case conReLU: ReLU =>
            ready2 := conReLU.io.valid
            add.io.biases := conReLU.io.result
          case conInitializer: Initializer =>
            ready2 := conInitializer.io.valid
            add.io.biases := conInitializer.io.output
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to an add module")
          case _ =>
            throw new Exception("Unknown module connected to add module inputs")
        }
        add.io.ready := ready1 && ready2
      case matMul: MatMul =>
        val connectedModule1 = modules(connections(0))
        val connectedModule2 = modules(connections(1))
        val ready1 = Wire(Bool())
        val ready2 = Wire(Bool())
        connectedModule1 match {
          case conInput: InputModule =>
            ready1 := conInput.io.valid
            matMul.io.inputs := conInput.io.outputs
          case conAdd: Add =>
            ready1 := conAdd.io.valid
            matMul.io.inputs := conAdd.io.result
          case conMatMul: MatMul =>
            ready1 := conMatMul.io.valid
            matMul.io.inputs := conMatMul.io.result
          case conReLU: ReLU =>
            ready1 := conReLU.io.valid
            matMul.io.inputs := conReLU.io.result
          case conInitializer: Initializer =>
            ready1 := conInitializer.io.valid
            matMul.io.inputs := conInitializer.io.output
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a matmul module")
          case _ =>
            throw new Exception("Unknown module connected to matmul module inputs")
        }
        connectedModule2 match {
          case conInput: InputModule =>
            ready2 := conInput.io.valid
            matMul.io.weights := conInput.io.outputs
          case conAdd: Add =>
            ready2 := conAdd.io.valid
            matMul.io.weights := conAdd.io.result
          case conMatMul: MatMul =>
            ready2 := conMatMul.io.valid
            matMul.io.weights := conMatMul.io.result
          case conReLU: ReLU =>
            ready2 := conReLU.io.valid
            matMul.io.weights := conReLU.io.result
          case conInitializer: Initializer =>
            ready2 := conInitializer.io.valid
            matMul.io.weights := conInitializer.io.output
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a matmul module")
          case _ =>
            throw new Exception("Unknown module connected to matmul module inputs")
        }
        matMul.io.ready := ready1 && ready2

      case relu: ReLU =>
        val connectedModule = modules(connections(0))
        connectedModule match {
          case conInput: InputModule =>
            relu.io.input := conInput.io.outputs
            relu.io.ready := conInput.io.valid
          case conAdd: Add =>
            relu.io.input := conAdd.io.result
            relu.io.ready := conAdd.io.valid
          case conMatMul: MatMul =>
            relu.io.input := conMatMul.io.result
            relu.io.ready := conMatMul.io.valid
          case conInitializer: Initializer =>
            relu.io.input := conInitializer.io.output
            relu.io.ready := conInitializer.io.valid
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a relu module")
          case _ =>
            throw new Exception("Unknown module connected to relu module")
        }

      case _: Initializer =>

      case output: OutputModule =>
        val connectedModule = modules(connections(0))
        connectedModule match {
          case conInput: InputModule =>
            output.io.inputs := conInput.io.outputs
            output.io.ready := conInput.io.valid
          case conAdd: Add =>
            output.io.inputs := conAdd.io.result
            output.io.ready := conAdd.io.valid
          case conMatMul: MatMul =>
            output.io.inputs := conMatMul.io.result
            output.io.ready := conMatMul.io.valid
          case conReLU: ReLU =>
            output.io.inputs := conReLU.io.result
            output.io.ready := conReLU.io.valid
          case conInitializer: Initializer =>
            output.io.inputs := conInitializer.io.output
            output.io.ready := conInitializer.io.valid
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
