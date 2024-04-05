import activation_functions.{ReLU, ReLU4d}
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
    val inputChannel = Flipped(new DecoupledIO(Vec(inputNode.dimensions._1, Vec(inputNode.dimensions._2,
      Vec(inputNode.dimensions._3, Vec(inputNode.dimensions._4, UInt(inputNode.w.W)))))))
    val outputChannel = new DecoupledIO(Vec(outputNode.dimensions._1, Vec(outputNode.dimensions._2,
      Vec(outputNode.dimensions._3, Vec(outputNode.dimensions._4, UInt(outputNode.w.W)))))
    )
  })

  val inputs = Wire(Vec(inputNode.dimensions._1, Vec(inputNode.dimensions._2, Vec(inputNode.dimensions._3,
    Vec(inputNode.dimensions._4, UInt(inputNode.w.W))))))
  val inputReady = Wire(Bool())
  val inputValid = Wire(Bool())

  val outputs = Wire(Vec(outputNode.dimensions._1, Vec(outputNode.dimensions._2, Vec(outputNode.dimensions._3,
    Vec(outputNode.dimensions._4, UInt(outputNode.w.W))))))
  val outputReady = Wire(Bool())
  val outputValid = Wire(Bool())


  // Module Creation
  val modules = listOfNodes.map {
    case inputType: InputType =>
      val input = Module(new InputModule(inputType))
      input
    case outputType: OutputType =>
      val output = Module(new OutputModule(outputType))
      output
    case initializerType: InitializerType =>
      val initializer = Module(new Initializer4d(initializerType))
      initializer
    case addType: AddType =>
      val add = Module(new Add4d(addType, enableDebuggingIO))
      add
    case matMulType: MatMulType =>
      val matMul = Module(new MatMul4d(matMulType, enableDebuggingIO))
      matMul
    case reluType: ReluType =>
      val relu = Module(new ReLU4d(reluType))
      relu
    case rounderType: RounderType =>
      val rounder = Module(new Rounder4d(rounderType))
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
        input.io.inputChannel.bits := inputs
        input.io.inputChannel.valid := inputValid
        inputReady := input.io.inputChannel.ready

      case add: Add4d =>
        val connectedModule1 = modules(connectionIndices.head)
        val connectedModule2 = modules(connectionIndices.last)
        if (printing) {
          println("Connected module 1: " + connectedModule1)
          println("Connected module 2: " + connectedModule2)
        }
        connectedModule1 match {
          case conInput: InputModule =>
            add.io.inputChannel <> conInput.io.outputChannel
          case conAdd: Add4d =>
            add.io.inputChannel <> conAdd.io.resultChannel
          case conMatMul: MatMul4d =>
            add.io.inputChannel <> conMatMul.io.outputChannel
          case conReLU: ReLU4d =>
            add.io.inputChannel <> conReLU.io.resultChannel
          case conInitializer: Initializer4d =>
            add.io.inputChannel <> conInitializer.io.outputChannel
          case conRound: Rounder4d =>
            add.io.inputChannel <> conRound.io.resultChannel
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to an add module")
          case _ =>
            throw new Exception("Unknown module connected to add module inputs")
        }
        connectedModule2 match {
          case conInput: InputModule =>
            add.io.biasChannel <> conInput.io.outputChannel
          case conAdd: Add4d =>
            add.io.biasChannel <> conAdd.io.resultChannel
          case conMatMul: MatMul4d =>
            add.io.biasChannel <> conMatMul.io.outputChannel
          case conReLU: ReLU4d =>
            add.io.biasChannel <> conReLU.io.resultChannel
          case conInitializer: Initializer4d =>
            add.io.biasChannel <> conInitializer.io.outputChannel
          case conRound: Rounder4d =>
            add.io.biasChannel <> conRound.io.resultChannel
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to an add module")
          case _ =>
            throw new Exception("Unknown module connected to add module inputs")
        }

      case matMul: MatMul4d =>
        val connectedModule1 = modules(connectionIndices.head)
        val connectedModule2 = modules(connectionIndices.last)
        if (printing) {
          println("Connected module 1: " + connectedModule1)
          println("Connected module 2: " + connectedModule2)
        }
        connectedModule1 match {
          case conInput: InputModule =>
            matMul.io.inputChannel <> conInput.io.outputChannel
          case conAdd: Add4d =>
            matMul.io.inputChannel <> conAdd.io.resultChannel
          case conMatMul: MatMul4d =>
            matMul.io.inputChannel <> conMatMul.io.outputChannel
          case conReLU: ReLU4d =>
            matMul.io.inputChannel <> conReLU.io.resultChannel
          case conInitializer: Initializer4d =>
            matMul.io.inputChannel <> conInitializer.io.outputChannel
          case conRound: Rounder4d =>
            matMul.io.inputChannel <> conRound.io.resultChannel
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a matmul module")
          case _ =>
            throw new Exception("Unknown module connected to matmul module inputs")
        }
        connectedModule2 match {
          case conInput: InputModule =>
            matMul.io.weightChannel <> conInput.io.outputChannel
          case conAdd: Add4d =>
            matMul.io.weightChannel <> conAdd.io.resultChannel
          case conMatMul: MatMul4d =>
            matMul.io.weightChannel <> conMatMul.io.outputChannel
          case conReLU: ReLU4d =>
            matMul.io.weightChannel <> conReLU.io.resultChannel
          case conInitializer: Initializer4d =>
            matMul.io.weightChannel <> conInitializer.io.outputChannel
          case conRound: Rounder4d =>
            matMul.io.weightChannel <> conRound.io.resultChannel
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a matmul module")
          case _ =>
            throw new Exception("Unknown module connected to matmul module inputs")
        }

      case relu: ReLU4d =>
        val connectedModule = modules(connectionIndices.head)
        if (printing) println("Connected module: " + connectedModule)
        connectedModule match {
          case conInput: InputModule =>
            relu.io.inputChannel <> conInput.io.outputChannel
          case conAdd: Add4d =>
            relu.io.inputChannel <> conAdd.io.resultChannel
          case conMatMul: MatMul4d =>
            relu.io.inputChannel <> conMatMul.io.outputChannel
          case conInitializer: Initializer4d =>
            relu.io.inputChannel <> conInitializer.io.outputChannel
          case conRound: Rounder4d =>
            relu.io.inputChannel <> conRound.io.resultChannel
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a relu module")
          case _ =>
            throw new Exception("Unknown module connected to relu module")
        }

      case _: Initializer4d =>
      // Initializers do not have any inputs

      case output: OutputModule =>
        // Should only happen once
        val connectedModule = modules(connectionIndices.head)
        if (printing) println("Connected module: " + connectedModule)
        connectedModule match {
          case conInput: InputModule =>
            output.io.inputChannel <> conInput.io.outputChannel
          case conAdd: Add4d =>
            output.io.inputChannel <> conAdd.io.resultChannel
          case conMatMul: MatMul4d =>
            output.io.inputChannel <> conMatMul.io.outputChannel
          case conReLU: ReLU4d =>
            output.io.inputChannel <> conReLU.io.resultChannel
          case conInitializer: Initializer4d =>
            output.io.inputChannel <> conInitializer.io.outputChannel
          case conRound: Rounder4d =>
            output.io.inputChannel <> conRound.io.resultChannel
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to an output module")
          case _ =>
            throw new Exception("Unknown module connected to output module")
        }
        outputs := output.io.outputChannel.bits
        outputValid := output.io.outputChannel.valid
        output.io.outputChannel.ready := outputReady

      case rounder: Rounder4d =>
        val connectedModule = modules(connectionIndices.head)
        if (printing) println("Connected module: " + connectedModule)
        connectedModule match {
          case conInput: InputModule =>
            rounder.io.inputChannel <> conInput.io.outputChannel
          case conAdd: Add4d =>
            rounder.io.inputChannel <> conAdd.io.resultChannel
          case conMatMul: MatMul4d =>
            rounder.io.inputChannel <> conMatMul.io.outputChannel
          case conReLU: ReLU4d =>
            rounder.io.inputChannel <> conReLU.io.resultChannel
          case conInitializer: Initializer4d =>
            rounder.io.inputChannel <> conInitializer.io.outputChannel
          case conRound: Rounder4d =>
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
      inputs := RegNext(io.inputChannel.bits)
      inputValid := RegNext(io.inputChannel.valid)
      io.inputChannel.ready := RegNext(inputReady)

      io.outputChannel.bits := RegNext(outputs)
      io.outputChannel.valid := RegNext(outputValid)
      outputReady := RegNext(io.outputChannel.ready)
    } else {
      inputs := io.inputChannel.bits
      inputValid := io.inputChannel.valid
      io.inputChannel.ready := inputReady

      io.outputChannel.bits := outputs
      io.outputChannel.valid := outputValid
      outputReady := io.outputChannel.ready
    }
  }


}
