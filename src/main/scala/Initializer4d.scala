import chisel3._

class Initializer4d(
                     val w: Int = 8,
                     val dimensions: (Int, Int, Int, Int) = (4, 4, 4, 4),
                     val data: Array[Array[Array[Array[Int]]]]
                   ) extends Module {


}
