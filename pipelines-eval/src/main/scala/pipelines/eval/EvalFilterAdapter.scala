package pipelines.eval

import pipelines.data.FilterAdapter

object EvalFilterAdapter {
  def apply(): FilterAdapter = JsonFilterAdapter().orElse(AvroFilterAdapter())
}
