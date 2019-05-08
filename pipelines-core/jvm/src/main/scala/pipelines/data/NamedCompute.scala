package pipelines.data


case class NamedCompute(name: String, compute: Compute) {
  override def toString = s"$name"
}