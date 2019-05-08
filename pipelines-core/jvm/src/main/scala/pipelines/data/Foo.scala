package pipelines.data
import monix.execution.Scheduler
import monix.reactive.Observable

// an example of implementing a sealed trait which is extended by ModifyMap
class Foo extends ModifyMap[Int, Long] {
  override def modify(input: Observable[Int])(implicit sched: Scheduler): Observable[Long] = input.map(_.toLong)
}
