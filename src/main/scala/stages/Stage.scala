package stages

import chisel3._
import chisel3.util.DecoupledIO

class Stage extends Module {
  var latency = 0
  var dspUsage = 0
}

class Stage0(
              wOut: Int,
              shapeOut: (Int, Int, Int, Int),
            ) extends Stage {

  val io = IO(new Bundle {
    val outputChannel = DecoupledIO(Vec(shapeOut._1, Vec(shapeOut._2, Vec(shapeOut._3, Vec(shapeOut._4, UInt(wOut.W))))))
  })
}

class Stage1(
              wIn: Int,
              shapeIn: (Int, Int, Int, Int),
              wOut: Int,
            ) extends Stage {

  lazy val shapeOut = shapeIn

  val io = IO(new Bundle {
    val inputChannel = Flipped(DecoupledIO(Vec(shapeIn._1, Vec(shapeIn._2, Vec(shapeIn._3, Vec(shapeIn._4, UInt(wIn.W)))))))
    val outputChannel = DecoupledIO(Vec(shapeOut._1, Vec(shapeOut._2, Vec(shapeOut._3, Vec(shapeOut._4, UInt(wOut.W))))))
  })
}

class Stage2(
              wIn1: Int,
              shapeIn1: (Int, Int, Int, Int),
              wIn2: Int,
              shapeIn2: (Int, Int, Int, Int),
              wOut: Int,
            ) extends Stage {

  lazy val shapeOut = shapeIn1

  val io = IO(new Bundle {
    val input1Channel = Flipped(DecoupledIO(Vec(shapeIn1._1, Vec(shapeIn1._2, Vec(shapeIn1._3, Vec(shapeIn1._4, UInt(wIn1.W)))))))
    val input2Channel = Flipped(DecoupledIO(Vec(shapeIn2._1, Vec(shapeIn2._2, Vec(shapeIn2._3, Vec(shapeIn2._4, UInt(wIn2.W)))))))
    val outputChannel = DecoupledIO(Vec(shapeOut._1, Vec(shapeOut._2, Vec(shapeOut._3, Vec(shapeOut._4, UInt(wOut.W))))))
  })
}
