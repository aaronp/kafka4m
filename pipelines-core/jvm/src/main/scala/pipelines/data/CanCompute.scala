package pipelines.data


trait CanCompute[T] {
  def compute(value: T): Compute
  def lift(desc: String, c: Compute): T
}
object CanCompute {
  implicit object identity extends CanCompute[Compute] {
    override def compute(value: Compute): Compute        = value
    override def lift(desc: String, c: Compute): Compute = c
  }
  implicit object named extends CanCompute[NamedCompute] {
    override def compute(value: NamedCompute): Compute        = value.compute
    override def lift(desc: String, c: Compute): NamedCompute = c.withName(desc)
  }
}
