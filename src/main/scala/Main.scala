import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{ChiselStage, FirtoolOption}
import gcd.GCD
object Main extends App {
  (new ChiselStage).execute(
    Array("--target", "systemverilog"),
    Seq(ChiselGeneratorAnnotation(() => new GCD()),
      FirtoolOption("--disable-all-randomization"))
  )
}