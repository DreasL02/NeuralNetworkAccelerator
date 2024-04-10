package scala_utils

object Optional {
  // for optional debug signals, as seen in https://groups.google.com/g/chisel-users/c/8XUcalmRp8M (visited 08-04-2024)
  def optional[T](enable: Boolean, value: T): Option[T] = {
    if (enable) Some(value) else None
  }
}
