package onnx

import chisel3._

case class Tensor(w: Int, dimensions: (Int, Int, Int, Int)) {
  // w is the bit-width of the tensor
  // dimensions._1 is the batch size
  // dimensions._2 is the channel size
  // dimensions._3 is the height of the tensor (number of rows)
  // dimensions._4 is the width of the tensor (number of columns)

  require(w > 0, "w must be greater than 0")
  require(dimensions._1 > 0, "dimensions._1 must be greater than 0")
  require(dimensions._2 > 0, "dimensions._2 must be greater than 0")
  require(dimensions._3 > 0, "dimensions._3 must be greater than 0")
  require(dimensions._4 > 0, "dimensions._4 must be greater than 0")
}
