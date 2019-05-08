package pipelines.eval

import io.circe.Json
import monix.execution.Scheduler
import monix.reactive.Observable
import pipelines.data.{ModifyObservable, ModifyObservableRequest, ModifyObservableResolver, ModifyObservableTyped}
import pipelines.expressions.{Cache, JsonExpressions}

class ModifyObservableJsonFilter(predicate: Json => Boolean) extends ModifyObservableTyped[Json] {
  override def modify(input: Observable[Json])(implicit sched: Scheduler): Observable[Json] = {
    input.filter(predicate)
  }
}

object ModifyObservableJsonFilter {
  case class Resolver(cache: Cache[Json => Boolean] = JsonExpressions.newCache) extends ModifyObservableResolver {
    override def resolve(request: ModifyObservableRequest): Option[ModifyObservable] = {
      request match {
        case ModifyObservableRequest.Filter(expression) =>
          cache(expression).toOption.map { predicate =>
            new ModifyObservableJsonFilter(predicate)
          }
        case _ => None
      }
    }
  }
}
