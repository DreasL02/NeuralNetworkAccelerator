package scala_utils

object Optional {
  def optional[T](enable: Boolean, value: T): Option[T] = { // for optional debug signals, https://groups.google.com/g/chisel-users/c/8XUcalmRp8M
    if (enable) Some(value) else None
  }
}
