package pipelines.eval

import monix.execution.Scheduler
import monix.reactive.Observable
import org.apache.avro.generic.IndexedRecord
import pipelines.data.{ModifyObservable, ModifyObservableRequest, ModifyObservableResolver, ModifyObservableTyped}
import pipelines.expressions.{AvroExpressions, Cache, Record}

object ModifyObservableAvroFilter {

  case class Resolver(cache: Cache[AvroExpressions.Predicate] = AvroExpressions.newCache) extends ModifyObservableResolver {
    override def resolve(request: ModifyObservableRequest): Option[ModifyObservable] = {
      request match {
        case ModifyObservableRequest.Filter(expression) =>
          cache(expression).toOption.map { predicate =>
            new ModifyObservableAvroFilter(predicate)
          }
        case _ => None
      }
    }
  }
}

class ModifyObservableAvroFilter(predicate : Record => Boolean) extends ModifyObservableTyped[IndexedRecord] {
  override def modify(input: Observable[Record])(implicit sched: Scheduler): Observable[Record] = {
    input.filter(predicate)
  }
}
