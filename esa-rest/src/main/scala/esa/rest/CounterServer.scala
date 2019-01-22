package esa.rest
import java.util.concurrent.atomic.AtomicInteger

import akka.http.scaladsl.server.Route
import endpoints.akkahttp.server
import endpoints.akkahttp.server.JsonSchemaEntities
import quickstart.{Counter, CounterEndpoints}
import akka.http.scaladsl.server._
import Directives._

class CounterServer(val value: AtomicInteger = new AtomicInteger(0)) extends CounterEndpoints with server.Endpoints with JsonSchemaEntities {

  val currentRoute: Route = currentValue.implementedBy { counter =>
    Counter(value.get())
  }

  val incRoute = increment.implementedBy { inc =>
    value.addAndGet(inc.step)
  }

  def routes = currentRoute ~ incRoute
}
