package pipelines.eval

trait DataSource[A] extends AutoCloseable {
  def data: A
}
