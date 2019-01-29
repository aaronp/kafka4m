package esa.rest.routes

import java.util.concurrent.atomic.AtomicInteger

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, _}
import endpoints.akkahttp.server
import endpoints.akkahttp.server.JsonSchemaEntities
import esa.endpoints.{Counter, CounterEndpoints}

class CounterRoutes(val value: AtomicInteger = new AtomicInteger(0)) extends CounterEndpoints with server.Endpoints with JsonSchemaEntities {

  val currentRoute: Route = currentValue.implementedBy { counter =>
    Counter(value.get())
  }

  val incRoute = increment.implementedBy { inc =>
    value.addAndGet(inc.step)
  }

  def routes = currentRoute ~ incRoute
}
