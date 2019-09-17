package kafka4m.util

final case class LatencySnapshot(perSecond: Int, total: Long) {
  override def toString = s"$perSecond / second, $total total"
  def nonEmpty          = perSecond > 0 && total > 0
}
