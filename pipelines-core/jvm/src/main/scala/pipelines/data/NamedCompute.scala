package pipelines.data

import pipelines.data.shape.Compute


case class NamedCompute(name: String, compute: Compute) {
  override def toString = s"$name"
}