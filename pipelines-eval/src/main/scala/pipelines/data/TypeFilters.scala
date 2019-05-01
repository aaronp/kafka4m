package pipelines.data

trait TypeFilters {
  def makePredicate[A](dataType: DataType, expr: String): Option[(A => Boolean)]
}
