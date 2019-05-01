package pipelines.eval

trait Provider[A] extends AutoCloseable {
  def data: A
}
