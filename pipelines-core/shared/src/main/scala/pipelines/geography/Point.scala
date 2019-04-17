package pipelines.geography

case class Point(x: Double, y: Double) {
  override def toString = s"($x,$y)"

  def translate(deltaX: Double = 0, deltaY: Double = 0): Point = {
    copy(x + deltaX, y + deltaY)
  }
}
