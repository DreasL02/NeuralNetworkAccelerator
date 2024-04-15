package TestingUtils

import scala.math.abs

object Comparison {
  // todo: find a better name, "isWithinThreshold"?
  def CompareWithErrorThreshold(a: Float, b: Float, threshold: Float): Boolean = {
    abs(a - b) <= threshold
  }
}
