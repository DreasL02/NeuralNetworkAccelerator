import activation_functions.ReLU
import chisel3._
import chisel3.util.DecoupledIO
import onnx.Operators._
import scala_utils.DimensionManipulation.{reverseRows, transpose}

class AutomaticGeneration(
                           listOfNodes: List[Any],
                           connectionList: List[List[Int]],
                           pipelineIO: Boolean = false,
                           enableDebuggingIO: Boolean = true,
                           printing: Boolean = true
                         ) extends Module {


  val inputNode = listOfNodes.head.asInstanceOf[InputType] // right now, the first node is always the input node
  val outputNode = listOfNodes.last.asInstanceOf[OutputType] // right now, the last node is always the output node


  val io = IO(new Bundle {
    // initializers
    val inputChannel = Flipped(new DecoupledIO(Vec(inputNode.dimensions._1, Vec(inputNode.dimensions._2, UInt(inputNode.w.W)))))
    val outputChannel = new DecoupledIO(Vec(outputNode.dimensions._1, Vec(outputNode.dimensions._2, UInt(outputNode.w.W))))
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
  for (i <- modules.indices) {
    val currentModule = modules(i)
    if (printing) println("Generating connections for: " + currentModule)
    val connectionIndices = connectionList(i)
    currentModule match {

      case input: InputModule =>
        // Should only happen once
        input.io.inputChannel <> io.inputChannel

      case add: Add =>
        val connectedModule1 = modules(connectionIndices.head)
        val connectedModule2 = modules(connectionIndices.last)
        if (printing) {
          println("Connected module 1: " + connectedModule1)
          println("Connected module 2: " + connectedModule2)
        }
        connectedModule1 match {
          case conInput: InputModule =>
            add.io.inputChannel <> conInput.io.outputChannel
          case conAdd: Add =>
            add.io.inputChannel <> conAdd.io.resultChannel
          case conMatMul: MatMul =>
            add.io.inputChannel <> conMatMul.io.resultChannel
          case conReLU: ReLU =>
            add.io.inputChannel <> conReLU.io.resultChannel
          case conInitializer: Initializer =>
            add.io.inputChannel <> conInitializer.io.outputChannel
          case conRound: Rounder =>
            add.io.inputChannel <> conRound.io.resultChannel
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to an add module")
          case _ =>
            throw new Exception("Unknown module connected to add module inputs")
        }
        connectedModule2 match {
          case conInput: InputModule =>
            add.io.biasChannel <> conInput.io.outputChannel
          case conAdd: Add =>
            add.io.biasChannel <> conAdd.io.resultChannel
          case conMatMul: MatMul =>
            add.io.biasChannel <> conMatMul.io.resultChannel
          case conReLU: ReLU =>
            add.io.biasChannel <> conReLU.io.resultChannel
          case conInitializer: Initializer =>
            add.io.biasChannel <> conInitializer.io.outputChannel
          case conRound: Rounder =>
            add.io.biasChannel <> conRound.io.resultChannel
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to an add module")
          case _ =>
            throw new Exception("Unknown module connected to add module inputs")
        }

      case matMul: MatMul =>
        val connectedModule1 = modules(connectionIndices.head)
        val connectedModule2 = modules(connectionIndices.last)
        if (printing) {
          println("Connected module 1: " + connectedModule1)
          println("Connected module 2: " + connectedModule2)
        }
        connectedModule1 match {
          case conInput: InputModule =>
            matMul.io.inputChannel <> conInput.io.outputChannel
          case conAdd: Add =>
            matMul.io.inputChannel <> conAdd.io.resultChannel
          case conMatMul: MatMul =>
            matMul.io.inputChannel <> conMatMul.io.resultChannel
          case conReLU: ReLU =>
            matMul.io.inputChannel <> conReLU.io.resultChannel
          case conInitializer: Initializer =>
            matMul.io.inputChannel <> conInitializer.io.outputChannel
          case conRound: Rounder =>
            matMul.io.inputChannel <> conRound.io.resultChannel
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a matmul module")
          case _ =>
            throw new Exception("Unknown module connected to matmul module inputs")
        }
        connectedModule2 match {
          case conInput: InputModule =>
            matMul.io.weightChannel <> conInput.io.outputChannel
          case conAdd: Add =>
            matMul.io.weightChannel <> conAdd.io.resultChannel
          case conMatMul: MatMul =>
            matMul.io.weightChannel <> conMatMul.io.resultChannel
          case conReLU: ReLU =>
            matMul.io.weightChannel <> conReLU.io.resultChannel
          case conInitializer: Initializer =>
            matMul.io.weightChannel <> conInitializer.io.outputChannel
          case conRound: Rounder =>
            matMul.io.weightChannel <> conRound.io.resultChannel
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a matmul module")
          case _ =>
            throw new Exception("Unknown module connected to matmul module inputs")
        }

      case relu: ReLU =>
        val connectedModule = modules(connectionIndices.head)
        if (printing) println("Connected module: " + connectedModule)
        connectedModule match {
          case conInput: InputModule =>
            relu.io.inputChannel <> conInput.io.outputChannel
          case conAdd: Add =>
            relu.io.inputChannel <> conAdd.io.resultChannel
          case conMatMul: MatMul =>
            relu.io.inputChannel <> conMatMul.io.resultChannel
          case conInitializer: Initializer =>
            relu.io.inputChannel <> conInitializer.io.outputChannel
          case conRound: Rounder =>
            relu.io.inputChannel <> conRound.io.resultChannel
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a relu module")
          case _ =>
            throw new Exception("Unknown module connected to relu module")
        }

      case _: Initializer =>
      // Initializers do not have any inputs

      case output: OutputModule =>
        // Should only happen once
        val connectedModule = modules(connectionIndices.head)
        if (printing) println("Connected module: " + connectedModule)
        connectedModule match {
          case conInput: InputModule =>
            output.io.inputChannel <> conInput.io.outputChannel
          case conAdd: Add =>
            output.io.inputChannel <> conAdd.io.resultChannel
          case conMatMul: MatMul =>
            output.io.inputChannel <> conMatMul.io.resultChannel
          case conReLU: ReLU =>
            output.io.inputChannel <> conReLU.io.resultChannel
          case conInitializer: Initializer =>
            output.io.inputChannel <> conInitializer.io.outputChannel
          case conRound: Rounder =>
            output.io.inputChannel <> conRound.io.resultChannel
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to an output module")
          case _ =>
            throw new Exception("Unknown module connected to output module")
        }
        output.io.outputChannel <> io.outputChannel


      case rounder: Rounder =>
        val connectedModule = modules(connectionIndices.head)
        if (printing) println("Connected module: " + connectedModule)
        connectedModule match {
          case conInput: InputModule =>
            rounder.io.inputChannel <> conInput.io.outputChannel
          case conAdd: Add =>
            rounder.io.inputChannel <> conAdd.io.resultChannel
          case conMatMul: MatMul =>
            rounder.io.inputChannel <> conMatMul.io.resultChannel
          case conReLU: ReLU =>
            rounder.io.inputChannel <> conReLU.io.resultChannel
          case conInitializer: Initializer =>
            rounder.io.inputChannel <> conInitializer.io.outputChannel
          case conRound: Rounder =>
            rounder.io.inputChannel <> conRound.io.resultChannel
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a rounder module")
          case _ =>
            throw new Exception("Unknown module connected to rounder module")
        }

      case _ =>
        throw new Exception("Unknown module type")
    }

    if (printing) println("Connections generated for: " + currentModule)

    if (pipelineIO) {
      println("PipelineIO is disabled for now")
    } else {

    }

  }
}
