package stages

sealed trait ConvImplementation

object ConvImplementation extends {
  case object Im2Col extends ConvImplementation

  case object Direct extends ConvImplementation
}

sealed trait MatMulImplementation

object MatMulImplementation extends {
  case object SystolicArray extends MatMulImplementation

  case object Direct extends MatMulImplementation
}

sealed trait InputImplementation

object InputImplementation extends {
  case object Uart extends InputImplementation

  case object Open extends InputImplementation
}

sealed trait OutputImplementation

object OutputImplementation extends {
  case object Uart extends OutputImplementation

  case object Open extends OutputImplementation
}

