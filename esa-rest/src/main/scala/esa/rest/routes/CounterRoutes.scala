package esa.rest.routes

import java.util.concurrent.atomic.AtomicInteger

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import esa.endpoints.{Counter, CounterEndpoints}

class CounterRoutes(val value: AtomicInteger = new AtomicInteger(0)) extends CounterEndpoints with BaseRoutes {

  val currentRoute: Route = currentValue.implementedBy { counter =>
    Counter(value.get())
  }

  val incRoute = increment.implementedBy { inc =>
    value.addAndGet(inc.step)
  }

  def routes = currentRoute ~ incRoute
}
