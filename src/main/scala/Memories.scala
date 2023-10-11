import chisel3._
import chisel3.util.log2Ceil
class Memories(w : Int = 8, dimension : Int = 4, initial_data_memory_state : Array[Int], initial_weights_memory_state : Array[Int], initial_bias_memory_state : Array[Int], initial_config_memory_state : Array[Int]) extends Module{
  val io = IO(new Bundle{
    val data_address = Input(UInt(log2Ceil(initial_data_memory_state.length).W))
    val weights_address = Input(UInt(log2Ceil(initial_weights_memory_state.length).W))
    val bias_address = Input(UInt(log2Ceil(initial_bias_memory_state.length).W))
    val config_address = Input(UInt(log2Ceil(initial_config_memory_state.length).W))

    val data_read = Output(Vec(dimension, UInt(w.W)))
    val weights_read = Output(Vec(dimension, UInt(w.W)))
    val bias_read = Output(Vec(dimension, UInt(w.W)))

    val config_read = Output(UInt((log2Ceil(w)+1).W))

    val write = Input(Bool())
    val data_write = Input(Vec(dimension, UInt(w.W)))
  })

  val data_memory = RegInit(VecInit(initial_data_memory_state.toIndexedSeq.map(_.S(w.W).asUInt)))
  val weights_memory = RegInit(VecInit(initial_weights_memory_state.toIndexedSeq.map(_.S(w.W).asUInt)))
  val bias_memory = RegInit(VecInit(initial_bias_memory_state.toIndexedSeq.map(_.S(w.W).asUInt)))

  val config_memory = RegInit(VecInit(initial_config_memory_state.toIndexedSeq.map(_.S((log2Ceil(w)+1).W).asUInt))) //fixed_point encoding + if signed

  for (i <- 0 until dimension) {
    io.data_read(i) := data_memory(io.data_address+i.U)
    io.weights_read(i) := weights_memory(io.weights_address+i.U)
    io.bias_read(i) := bias_memory(io.bias_address+i.U)

    when(io.write) {
      data_memory(io.data_address + i.U) := io.data_write(i)
    }
  }

  io.config_read := config_memory(io.config_address)
}
