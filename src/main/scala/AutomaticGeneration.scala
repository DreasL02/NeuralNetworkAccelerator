import activation_functions.ReLU
import chisel3._
import onnx.Operators._
import scala_utils.DimensionManipulation.{reverseRows, transpose}
import systolic_array.MatMul

class AutomaticGeneration(
                           listOfNodes: List[Any],
                           connectionList: List[List[Int]],
                           enableDebuggingIO: Boolean = true,
                           printing: Boolean = true
                         ) extends Module {

  val inputNode = listOfNodes.head.asInstanceOf[InputType] // right now, the first node is always the input node
  val outputNode = listOfNodes.last.asInstanceOf[OutputType] // right now, the last node is always the output node

  val io = IO(new Bundle {
    // initializers
    val ready = Input(Bool()) // indicates that the producer has new data to be processed
    val input = Input(Vec(inputNode.dimensions._1, Vec(inputNode.dimensions._2, UInt(inputNode.w.W))))

    val output = Output(Vec(outputNode.dimensions._1, Vec(outputNode.dimensions._2, UInt(outputNode.w.W))))
    val valid = Output(Bool()) // indicates that the module should be done
  })

  // Module Creation
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
    case roundType: RoundType =>
      val rounder = Module(new Rounder(roundType))
      rounder
    case _ =>
      throw new Exception("Unknown specified module type (module creation)")
  }

  // Connection Logic (Wiring)
  for (i <- 0 until modules.length) {
    val currentModule = modules(i)
    if (printing) println("Generating connections for: " + currentModule)
    val connectionIndices = connectionList(i)
    currentModule match {
      case input: InputModule =>
        // Should only happen once
        input.io.ready := io.ready
        input.io.inputs := io.input
      case add: Add =>
        val connectedModule1 = modules(connectionIndices(0))
        val connectedModule2 = modules(connectionIndices(1))
        if (printing) {
          println("Connected module 1: " + connectedModule1)
          println("Connected module 2: " + connectedModule2)
        }
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
          case conRound: Rounder =>
            ready1 := conRound.io.valid
            add.io.input := conRound.io.output
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
          case conRound: Rounder =>
            ready2 := conRound.io.valid
            add.io.biases := conRound.io.output
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to an add module")
          case _ =>
            throw new Exception("Unknown module connected to add module inputs")
        }
        add.io.ready := ready1 && ready2
      case matMul: MatMul =>
        val connectedModule1 = modules(connectionIndices(0))
        val connectedModule2 = modules(connectionIndices(1))
        if (printing) {
          println("Connected module 1: " + connectedModule1)
          println("Connected module 2: " + connectedModule2)
        }
        val ready1 = Wire(Bool())
        val ready2 = Wire(Bool())
        connectedModule1 match {
          case conInput: InputModule =>
            ready1 := conInput.io.valid
            matMul.io.inputs := reverseRows(conInput.io.outputs)
          case conAdd: Add =>
            ready1 := conAdd.io.valid
            matMul.io.inputs := reverseRows(conAdd.io.result)
          case conMatMul: MatMul =>
            ready1 := conMatMul.io.valid
            matMul.io.inputs := reverseRows(conMatMul.io.result)
          case conReLU: ReLU =>
            ready1 := conReLU.io.valid
            matMul.io.inputs := reverseRows(conReLU.io.result)
          case conInitializer: Initializer =>
            ready1 := conInitializer.io.valid
            matMul.io.inputs := reverseRows(conInitializer.io.output)
          case conRound: Rounder =>
            ready1 := conRound.io.valid
            matMul.io.inputs := reverseRows(conRound.io.output)
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a matmul module")
          case _ =>
            throw new Exception("Unknown module connected to matmul module inputs")
        }
        connectedModule2 match {
          case conInput: InputModule =>
            ready2 := conInput.io.valid
            matMul.io.weights := transpose(conInput.io.outputs)
          case conAdd: Add =>
            ready2 := conAdd.io.valid
            matMul.io.weights := transpose(conAdd.io.result)
          case conMatMul: MatMul =>
            ready2 := conMatMul.io.valid
            matMul.io.weights := transpose(conMatMul.io.result)
          case conReLU: ReLU =>
            ready2 := conReLU.io.valid
            matMul.io.weights := transpose(conReLU.io.result)
          case conInitializer: Initializer =>
            ready2 := conInitializer.io.valid
            matMul.io.weights := transpose(conInitializer.io.output)
          case conRound: Rounder =>
            ready2 := conRound.io.valid
            matMul.io.weights := transpose(conRound.io.output)
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a matmul module")
          case _ =>
            throw new Exception("Unknown module connected to matmul module inputs")
        }
        matMul.io.ready := ready1 && ready2

      case relu: ReLU =>
        val connectedModule = modules(connectionIndices(0))
        if (printing) println("Connected module: " + connectedModule)
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
          case conRound: Rounder =>
            relu.io.input := conRound.io.output
            relu.io.ready := conRound.io.valid
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a relu module")
          case _ =>
            throw new Exception("Unknown module connected to relu module")
        }

      case _: Initializer =>
      // Initializers do not have any inputs

      case output: OutputModule =>
        // Should only happen once
        val connectedModule = modules(connectionIndices(0))
        if (printing) println("Connected module: " + connectedModule)
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
          case conRound: Rounder =>
            output.io.inputs := conRound.io.output
            output.io.ready := conRound.io.valid
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to an output module")
          case _ =>
            throw new Exception("Unknown module connected to output module")
        }
        io.output := output.io.outputs
        io.valid := output.io.valid

      case rounder: Rounder =>
        val connectedModule = modules(connectionIndices(0))
        if (printing) println("Connected module: " + connectedModule)
        connectedModule match {
          case conInput: InputModule =>
            rounder.io.input := conInput.io.outputs
            rounder.io.ready := conInput.io.valid
          case conAdd: Add =>
            rounder.io.input := conAdd.io.result
            rounder.io.ready := conAdd.io.valid
          case conMatMul: MatMul =>
            rounder.io.input := conMatMul.io.result
            rounder.io.ready := conMatMul.io.valid
          case conReLU: ReLU =>
            rounder.io.input := conReLU.io.result
            rounder.io.ready := conReLU.io.valid
          case conInitializer: Initializer =>
            rounder.io.input := conInitializer.io.output
            rounder.io.ready := conInitializer.io.valid
          case conRound: Rounder =>
            rounder.io.input := conRound.io.output
            rounder.io.ready := conRound.io.valid
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a rounder module")
          case _ =>
            throw new Exception("Unknown module connected to rounder module")
        }

      case _ =>
        throw new Exception("Unknown module type")
    }

    if (printing) println("Connections generated for: " + currentModule)
  }
}
