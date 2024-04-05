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
    case reshapeType: ReshapeType =>
      val reshape = Module(new Reshape(reshapeType))
      reshape
    case convType: ConvType =>
      val conv = Module(new Conv4d(convType))
      conv
    case maxPoolType: MaxPoolType =>
      val maxPool = Module(new MaxPool4d(maxPoolType))
      maxPool
    case broadcasterType: BroadcasterType =>
      val broadcaster = Module(new Broadcaster(broadcasterType))
      broadcaster
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
          case conReshape: Reshape =>
            add.io.inputChannel <> conReshape.io.resultChannel
          case conConv: Conv4d =>
            add.io.inputChannel <> conConv.io.outputChannel
          case conMaxPool: MaxPool4d =>
            add.io.inputChannel <> conMaxPool.io.resultChannel
          case conBroadcaster: Broadcaster =>
            add.io.inputChannel <> conBroadcaster.io.resultChannel
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
          case conReshape: Reshape =>
            add.io.biasChannel <> conReshape.io.resultChannel
          case conConv: Conv4d =>
            add.io.biasChannel <> conConv.io.outputChannel
          case conMaxPool: MaxPool4d =>
            add.io.biasChannel <> conMaxPool.io.resultChannel
          case conBroadcaster: Broadcaster =>
            add.io.biasChannel <> conBroadcaster.io.resultChannel
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
          case conReshape: Reshape =>
            matMul.io.inputChannel <> conReshape.io.resultChannel
          case conConv: Conv4d =>
            matMul.io.inputChannel <> conConv.io.outputChannel
          case conMaxPool: MaxPool4d =>
            matMul.io.inputChannel <> conMaxPool.io.resultChannel
          case conBroadcaster: Broadcaster =>
            matMul.io.inputChannel <> conBroadcaster.io.resultChannel
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
          case conReshape: Reshape =>
            matMul.io.weightChannel <> conReshape.io.resultChannel
          case conConv: Conv4d =>
            matMul.io.weightChannel <> conConv.io.outputChannel
          case conMaxPool: MaxPool4d =>
            matMul.io.weightChannel <> conMaxPool.io.resultChannel
          case conBroadcaster: Broadcaster =>
            matMul.io.weightChannel <> conBroadcaster.io.resultChannel
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
          case conReshape: Reshape =>
            relu.io.inputChannel <> conReshape.io.resultChannel
          case conConv: Conv4d =>
            relu.io.inputChannel <> conConv.io.outputChannel
          case conMaxPool: MaxPool4d =>
            relu.io.inputChannel <> conMaxPool.io.resultChannel
          case conBroadcaster: Broadcaster =>
            relu.io.inputChannel <> conBroadcaster.io.resultChannel
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
          case conReshape: Reshape =>
            output.io.inputChannel <> conReshape.io.resultChannel
          case conConv: Conv4d =>
            output.io.inputChannel <> conConv.io.outputChannel
          case conMaxPool: MaxPool4d =>
            output.io.inputChannel <> conMaxPool.io.resultChannel
          case conBroadcaster: Broadcaster =>
            output.io.inputChannel <> conBroadcaster.io.resultChannel
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
          case conReshape: Reshape =>
            rounder.io.inputChannel <> conReshape.io.resultChannel
          case conConv: Conv4d =>
            rounder.io.inputChannel <> conConv.io.outputChannel
          case conMaxPool: MaxPool4d =>
            rounder.io.inputChannel <> conMaxPool.io.resultChannel
          case conBroadcaster: Broadcaster =>
            rounder.io.inputChannel <> conBroadcaster.io.resultChannel
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a rounder module")
          case _ =>
            throw new Exception("Unknown module connected to rounder module")
        }

      case reshape: Reshape =>
        val connectedModule1 = modules(connectionIndices.head)
        val connectedModule2 = modules(connectionIndices.last)
        if (printing) {
          println("Connected module 1: " + connectedModule1)
          println("Connected module 2: " + connectedModule2)
        }
        connectedModule1 match {
          case conInput: InputModule =>
            reshape.io.inputChannel <> conInput.io.outputChannel
          case conAdd: Add4d =>
            reshape.io.inputChannel <> conAdd.io.resultChannel
          case conMatMul: MatMul4d =>
            reshape.io.inputChannel <> conMatMul.io.outputChannel
          case conReLU: ReLU4d =>
            reshape.io.inputChannel <> conReLU.io.resultChannel
          case conInitializer: Initializer4d =>
            reshape.io.inputChannel <> conInitializer.io.outputChannel
          case conRound: Rounder4d =>
            reshape.io.inputChannel <> conRound.io.resultChannel
          case conReshape: Reshape =>
            reshape.io.inputChannel <> conReshape.io.resultChannel
          case conConv: Conv4d =>
            reshape.io.inputChannel <> conConv.io.outputChannel
          case conMaxPool: MaxPool4d =>
            reshape.io.inputChannel <> conMaxPool.io.resultChannel
          case conBroadcaster: Broadcaster =>
            reshape.io.inputChannel <> conBroadcaster.io.resultChannel
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a reshape module")
          case _ =>
            throw new Exception("Unknown module connected to reshape module inputs")
        }
        connectedModule2 match {
          case _: InputModule =>
            throw new Exception("Input module cannot be connected to a reshape shape")
          case _: Add4d =>
            throw new Exception("Add module cannot be connected to a reshape shape")
          case _: MatMul4d =>
            throw new Exception("MatMul module cannot be connected to a reshape shape")
          case _: ReLU4d =>
            throw new Exception("ReLU module cannot be connected to a reshape shape")
          case conInitializer: Initializer4d =>
            reshape.io.shapeChannel <> conInitializer.io.outputChannel
          case _: Rounder4d =>
            throw new Exception("Rounder module cannot be connected to a reshape shape")
          case _: Reshape =>
            throw new Exception("Reshape module cannot be connected to a reshape shape")
          case _: Conv4d =>
            throw new Exception("Conv module cannot be connected to a reshape shape")
          case _: MaxPool4d =>
            throw new Exception("Maxpool module cannot be connected to a reshape shape")
          case _: Broadcaster =>
            throw new Exception("Broadcaster cannot be connected to a reshape shape")
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a reshape shape")
          case _ =>
            throw new Exception("Unknown module connected to reshape module inputs")
        }


      case conv: Conv4d =>
        val connectedModule1 = modules(connectionIndices.head)
        val connectedModule2 = modules(connectionIndices.last)
        if (printing) {
          println("Connected module 1: " + connectedModule1)
          println("Connected module 2: " + connectedModule2)
        }
        connectedModule1 match {
          case conInput: InputModule =>
            conv.io.inputChannel <> conInput.io.outputChannel
          case conAdd: Add4d =>
            conv.io.inputChannel <> conAdd.io.resultChannel
          case conMatMul: MatMul4d =>
            conv.io.inputChannel <> conMatMul.io.outputChannel
          case conReLU: ReLU4d =>
            conv.io.inputChannel <> conReLU.io.resultChannel
          case conInitializer: Initializer4d =>
            conv.io.inputChannel <> conInitializer.io.outputChannel
          case conRound: Rounder4d =>
            conv.io.inputChannel <> conRound.io.resultChannel
          case conReshape: Reshape =>
            conv.io.inputChannel <> conReshape.io.resultChannel
          case conConv: Conv4d =>
            conv.io.inputChannel <> conConv.io.outputChannel
          case conMaxPool: MaxPool4d =>
            conv.io.inputChannel <> conMaxPool.io.resultChannel
          case conBroadcaster: Broadcaster =>
            conv.io.inputChannel <> conBroadcaster.io.resultChannel
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a conv module")
          case _ =>
            throw new Exception("Unknown module connected to conv module inputs")
        }
        connectedModule2 match {
          case conInput: InputModule =>
            conv.io.kernelChannel <> conInput.io.outputChannel
          case conAdd: Add4d =>
            conv.io.kernelChannel <> conAdd.io.resultChannel
          case conMatMul: MatMul4d =>
            conv.io.kernelChannel <> conMatMul.io.outputChannel
          case conReLU: ReLU4d =>
            conv.io.kernelChannel <> conReLU.io.resultChannel
          case conInitializer: Initializer4d =>
            conv.io.kernelChannel <> conInitializer.io.outputChannel
          case conRound: Rounder4d =>
            conv.io.kernelChannel <> conRound.io.resultChannel
          case conReshape: Reshape =>
            conv.io.kernelChannel <> conReshape.io.resultChannel
          case conConv: Conv4d =>
            conv.io.kernelChannel <> conConv.io.outputChannel
          case conMaxPool: MaxPool4d =>
            conv.io.kernelChannel <> conMaxPool.io.resultChannel
          case conBroadcaster: Broadcaster =>
            conv.io.kernelChannel <> conBroadcaster.io.resultChannel
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a conv module")
          case _ =>
            throw new Exception("Unknown module connected to conv module inputs")
        }

      case maxPool: MaxPool4d =>
        val connectedModule = modules(connectionIndices.head)
        if (printing) println("Connected module: " + connectedModule)
        connectedModule match {
          case conInput: InputModule =>
            maxPool.io.inputChannel <> conInput.io.outputChannel
          case conAdd: Add4d =>
            maxPool.io.inputChannel <> conAdd.io.resultChannel
          case conMatMul: MatMul4d =>
            maxPool.io.inputChannel <> conMatMul.io.outputChannel
          case conReLU: ReLU4d =>
            maxPool.io.inputChannel <> conReLU.io.resultChannel
          case conInitializer: Initializer4d =>
            maxPool.io.inputChannel <> conInitializer.io.outputChannel
          case conRound: Rounder4d =>
            maxPool.io.inputChannel <> conRound.io.resultChannel
          case conReshape: Reshape =>
            maxPool.io.inputChannel <> conReshape.io.resultChannel
          case conConv: Conv4d =>
            maxPool.io.inputChannel <> conConv.io.outputChannel
          case conMaxPool: MaxPool4d =>
            maxPool.io.inputChannel <> conMaxPool.io.resultChannel
          case conBroadcaster: Broadcaster =>
            maxPool.io.inputChannel <> conBroadcaster.io.resultChannel
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a maxpool module")
          case _ =>
            throw new Exception("Unknown module connected to maxpool module")
        }

      case broadcaster: Broadcaster =>
        val connectedModule = modules(connectionIndices.head)
        if (printing) println("Connected module: " + connectedModule)
        connectedModule match {
          case conInput: InputModule =>
            broadcaster.io.inputChannel <> conInput.io.outputChannel
          case conAdd: Add4d =>
            broadcaster.io.inputChannel <> conAdd.io.resultChannel
          case conMatMul: MatMul4d =>
            broadcaster.io.inputChannel <> conMatMul.io.outputChannel
          case conReLU: ReLU4d =>
            broadcaster.io.inputChannel <> conReLU.io.resultChannel
          case conInitializer: Initializer4d =>
            broadcaster.io.inputChannel <> conInitializer.io.outputChannel
          case conRound: Rounder4d =>
            broadcaster.io.inputChannel <> conRound.io.resultChannel
          case conReshape: Reshape =>
            broadcaster.io.inputChannel <> conReshape.io.resultChannel
          case conConv: Conv4d =>
            broadcaster.io.inputChannel <> conConv.io.outputChannel
          case conMaxPool: MaxPool4d =>
            broadcaster.io.inputChannel <> conMaxPool.io.resultChannel
          case conBroadcaster: Broadcaster =>
            broadcaster.io.inputChannel <> conBroadcaster.io.resultChannel
          case _: OutputModule =>
            throw new Exception("Output module cannot be connected to a broadcaster module")
          case _ =>
            throw new Exception("Unknown module connected to broadcaster module")
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
