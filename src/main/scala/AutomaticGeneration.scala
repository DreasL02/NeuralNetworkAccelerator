import chisel3._
import chisel3.util.{DecoupledIO, MixedVec}
import onnx.Operators._
import operators._
import stages._

class AutomaticGeneration(
                           listOfNodes: List[Any],
                           connectionList: List[List[Int]],
                           printing: Boolean = false
                         ) extends Module {
  var latency = 0
  var dspUsage = 0

  private val inputs = listOfNodes.filter(_.isInstanceOf[InputType])
  private val automaticInputChannels = for (i <- inputs.indices) yield {
    val inputChannels = Flipped(DecoupledIO(Vec(inputs(i).asInstanceOf[InputType].inputShape._1, Vec(inputs(i).asInstanceOf[InputType].inputShape._2, Vec(inputs(i).asInstanceOf[InputType].inputShape._3, Vec(inputs(i).asInstanceOf[InputType].inputShape._4, UInt(inputs(i).asInstanceOf[InputType].wIn.W)))))))
    inputChannels
  }

  private val outputs = listOfNodes.filter(_.isInstanceOf[OutputType])
  private val automaticOutputChannels = for (i <- outputs.indices) yield {
    val outputChannel = DecoupledIO(Vec(outputs(i).asInstanceOf[OutputType].outputShape._1, Vec(outputs(i).asInstanceOf[OutputType].outputShape._2, Vec(outputs(i).asInstanceOf[OutputType].outputShape._3, Vec(outputs(i).asInstanceOf[OutputType].outputShape._4, UInt(outputs(i).asInstanceOf[OutputType].wOut.W))))))
    outputChannel
  }

  val io = IO(new Bundle {
    val inputChannels = MixedVec(automaticInputChannels)
    val outputChannels = MixedVec(automaticOutputChannels)
  })

  // Module Creation
  val stages: List[Stage] = listOfNodes.map {
    case inputType: InputType =>
      val input = Module(new InputStage(inputType))
      input
    case outputType: OutputType =>
      val output = Module(new OutputStage(outputType))
      output
    case initializerType: InitializerType =>
      val initializer = Module(new InitializerStage(initializerType))
      initializer
    case addType: AddType =>
      val add = Module(new AddStage(addType))
      add
    case matMulType: MatMulType =>
      val matMul = Module(new MatMulStage(matMulType))
      matMul
    case reluType: ReluType =>
      val relu = Module(new ReLUStage(reluType))
      relu
    case rounderType: RounderType =>
      val rounder = Module(new RounderStage(rounderType))
      rounder
    case reshapeType: ReshapeType =>
      val reshape = Module(new ReshapeStage(reshapeType))
      reshape
    case convType: ConvType =>
      val conv = Module(new ConvStage(convType))
      conv
    case maxPoolType: MaxPoolType =>
      val maxPool = Module(new MaxPoolStage(maxPoolType))
      maxPool
    case broadcasterType: BroadcasterType =>
      val broadcaster = Module(new BroadcasterStage(broadcasterType))
      broadcaster
    case _ =>
      throw new Exception("Unknown specified module type (module creation)")
  }

  stages.foreach { stage =>
    latency += stage.latency
    dspUsage += stage.dspUsage
  }

  if (printing) {
    println("====================================")
    println("Total estimated latency: " + latency)
    println("Total estimated DSP usage: " + dspUsage)
    println("====================================")
  }

  if (printing) println("Modules Initialized. Beginning connection logic.")

  private var inputIndex = 0
  private var outputIndex = 0

  // Connection Logic (Wiring)
  for (i <- stages.indices) {
    val currentStage = stages(i)
    if (printing) println("Generating connections for: " + currentStage)
    val connectionIndices = connectionList(i)
    currentStage match {
      case input: InputStage =>
        if (printing) println("Connecting to input channel")
        // Should only happen once
        input.io.inputChannel <> io.inputChannels(inputIndex)
        inputIndex += 1

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
        io.outputChannels(outputIndex) <> output.io.outputChannel
        outputIndex += 1

      case _: Stage0 =>
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
