import chisel3._
import chisel3.util.DecoupledIO
import onnx.Operators._
import operators._
import stages._

class AutomaticGeneration(
                           listOfNodes: List[Any],
                           connectionList: List[List[Int]],
                           printing: Boolean = false
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

  var latency = 0
  var dspUsage = 0

  // Module Creation
  val stages: List[Stage] = listOfNodes.map {
    case inputType: InputType =>
      val input = Module(new InputStage(inputType))
      latency += input.latency
      dspUsage += input.dspUsage
      input
    case outputType: OutputType =>
      val output = Module(new OutputStage(outputType))
      latency += output.latency
      dspUsage += output.dspUsage
      output
    case initializerType: InitializerType =>
      val initializer = Module(new InitializerStage(initializerType))
      latency += initializer.latency
      dspUsage += initializer.dspUsage
      initializer
    case addType: AddType =>
      val add = Module(new AddStage(addType))
      latency += add.latency
      dspUsage += add.dspUsage
      add
    case matMulType: MatMulType =>
      val matMul = Module(new MatMulStage(matMulType))
      latency += matMul.latency
      dspUsage += matMul.dspUsage
      matMul
    case reluType: ReluType =>
      val relu = Module(new ReLUStage(reluType))
      latency += relu.latency
      dspUsage += relu.dspUsage
      relu
    case rounderType: RounderType =>
      val rounder = Module(new RounderStage(rounderType))
      latency += rounder.latency
      dspUsage += rounder.dspUsage
      rounder
    case reshapeType: ReshapeType =>
      val reshape = Module(new ReshapeStage(reshapeType))
      latency += reshape.latency
      dspUsage += reshape.dspUsage
      reshape
    case convType: ConvType =>
      val conv = Module(new ConvStage(convType))
      latency += conv.latency
      dspUsage += conv.dspUsage
      conv
    case maxPoolType: MaxPoolType =>
      val maxPool = Module(new MaxPoolStage(maxPoolType))
      latency += maxPool.latency
      dspUsage += maxPool.dspUsage
      maxPool
    case broadcasterType: BroadcasterType =>
      val broadcaster = Module(new BroadcasterStage(broadcasterType))
      latency += broadcaster.latency
      dspUsage += broadcaster.dspUsage
      broadcaster
    case _ =>
      throw new Exception("Unknown specified module type (module creation)")
  }

  if (printing) {
    println("====================================")
    println("Total estimated latency: " + latency)
    println("Total estimated DSP usage: " + dspUsage)
    println("====================================")
  }

  if (printing) println("Modules Initialized. Beginning connection logic.")

  // Connection Logic (Wiring)
  for (i <- stages.indices) {
    val currentStage = stages(i)
    if (printing) println("Generating connections for: " + currentStage)
    val connectionIndices = connectionList(i)
    currentStage match {
      case input: InputStage =>
        if (printing) println("Connecting to input channel")
        // Should only happen once
        input.io.inputChannel <> io.inputChannel

      case output: OutputStage =>
        // Should only happen once
        if (printing) {
          println("Connecting to module: " + stages(connectionIndices.head) + " index: " + connectionIndices.head)
        }
        stages(connectionIndices.head) match {
          case conStage0: Stage0 =>
            output.io.inputChannel <> conStage0.io.outputChannel
          case conStage1: Stage1 =>
            output.io.inputChannel <> conStage1.io.outputChannel
          case conStage2: Stage2 =>
            output.io.inputChannel <> conStage2.io.outputChannel
          case _ =>
            throw new Exception("Unknown stage type")
        }
        if (printing) println("Connecting to output channel")
        io.outputChannel <> output.io.outputChannel

      case stage0: Stage0 =>
      // Does not have any input channels, so no need to connect anything
      case stage1: Stage1 =>
        if (printing) {
          println("Connecting to module: " + stages(connectionIndices.head) + " index: " + connectionIndices.head)
        }
        stages(connectionIndices.head) match {
          case conStage0: Stage0 =>
            stage1.io.inputChannel <> conStage0.io.outputChannel
          case conStage1: Stage1 =>
            stage1.io.inputChannel <> conStage1.io.outputChannel
          case conStage2: Stage2 =>
            stage1.io.inputChannel <> conStage2.io.outputChannel
          case _ =>
            throw new Exception("Unknown stage type")
        }

      case stage2: Stage2 =>
        if (printing) {
          println("Connecting to module 1: " + stages(connectionIndices.head) + " index: " + connectionIndices.head)
          println("Connecting to module 2: " + stages(connectionIndices.last) + " index: " + connectionIndices.last)
        }

        stages(connectionIndices.head) match {
          case conStage0: Stage0 =>
            stage2.io.input1Channel <> conStage0.io.outputChannel
          case conStage1: Stage1 =>
            stage2.io.input1Channel <> conStage1.io.outputChannel
          case conStage2: Stage2 =>
            stage2.io.input1Channel <> conStage2.io.outputChannel
          case _ =>
            throw new Exception("Unknown stage type")
        }

        stages(connectionIndices.last) match {
          case conStage0: Stage0 =>
            stage2.io.input2Channel <> conStage0.io.outputChannel
          case conStage1: Stage1 =>
            stage2.io.input2Channel <> conStage1.io.outputChannel
          case conStage2: Stage2 =>
            stage2.io.input2Channel <> conStage2.io.outputChannel
          case _ =>
            throw new Exception("Unknown stage type")
        }

      case _ =>
        throw new Exception("Unknown stage type")
    }

    if (printing) println("Connections generated for: " + currentStage)


  }

  if (printing) println("Connections done.")
}
