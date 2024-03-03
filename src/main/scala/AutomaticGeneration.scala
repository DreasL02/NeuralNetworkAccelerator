import activation_functions.ReLU
import chisel3._
import onnx.Operators._
import systolic_array.MatMul

class AutomaticGeneration(
                           listOfNodes: List[Any],
                           enableDebuggingIO: Boolean = true
                         ) extends Module {
  val modules = listOfNodes.map {
    case inputType: InputType =>
      val input = Module(new Input(inputType))
      input
    case outputType: OutputType =>
      val output = Module(new Output(outputType))
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

  val io = IO(new Bundle {
    // initializers
    val ready = Input(Bool()) // indicates that the producer has new data to be processed
    val valid = Output(Bool()) // indicates that the module should be done

    val input = Input(Vec(inputType.dimensions._1, Vec(inputType.dimensions._2, UInt(inputType.w.W))))
    val output = Output(Vec(outputType.dimensions._1, Vec(outputType.dimensions._2, UInt(outputType.w.W))))
  })


}
