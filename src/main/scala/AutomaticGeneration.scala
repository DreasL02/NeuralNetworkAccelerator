import activation_functions.ReLU
import chisel3._
import onnx.Operators._
import systolic_array.MatMul

class AutomaticGeneration(
                           inputType: InputType,
                           listOfInitializers: List[initializerType],
                           listOfMatMul: List[MatMulType],
                           listOfAdd: List[AddType],
                           listOfReLU: List[ReLUType],
                           outputType: OutputType,
                           connectionList: List[(String, String)], //
                           enableDebuggingIO: Boolean = true
                         ) extends Module {

  val io = IO(new Bundle {
    // initializers
    val ready = Input(Bool()) // indicates that the producer has new data to be processed
    val valid = Output(Bool()) // indicates that the module should be done

    val input = Input(Vec(inputType.dimensions._1, Vec(inputType.dimensions._2, UInt(inputType.w.W))))
    val output = Output(Vec(outputType.dimensions._1, Vec(outputType.dimensions._2, UInt(outputType.w.W))))
  })

  val initializerModules = for (initializer <- listOfInitializers) yield {
    Module(new Initializer(initializer.w, initializer.dimensions._1, initializer.dimensions._2, initializer.data))
  }

  val addModules = for (add <- listOfAdd) yield {
    Module(new Add(add.wOperands, add.operandDimensions._1, add.operandDimensions._2, enableDebuggingIO))
  }

  val matMulModules = for (matMul <- listOfMatMul) yield {
    Module(new MatMul(matMul.wOperands, matMul.wResult, matMul.operandADimensions._1, matMul.operandBDimensions._2,
      matMul.operandBDimensions._1, matMul.signed, enableDebuggingIO))
  }

  val reLUModules = for (reLU <- listOfReLU) yield {
    Module(new ReLU(reLU.wOperands, reLU.operandDimensions._1, reLU.operandDimensions._2, reLU.signed))
  }

  // Connect modules


}
