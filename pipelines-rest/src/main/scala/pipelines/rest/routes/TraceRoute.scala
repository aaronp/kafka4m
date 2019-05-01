package pipelines.rest.routes

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.directives.BasicDirectives.{extractRequestContext, mapRouteResult}
import akka.http.scaladsl.server.{RouteResult, _}
import com.typesafe.scalalogging.StrictLogging

/**
  * A route which can wrap incoming/outgoing messages for support, metrics, etc.
  */
trait TraceRoute {
  def onRequest(request: HttpRequest): Unit
  def onResponse(request: HttpRequest, requestTime: Long, response: RouteResult): Unit

  final def wrap: Directive0 = extractRequestContext.tflatMap { ctxt =>
    onRequest(ctxt._1.request)
    val started = System.currentTimeMillis
    mapRouteResult { result: RouteResult =>
      onResponse(ctxt._1.request, started, result)
      result
    }
  }

}

object TraceRoute {

  def pretty(r: HttpRequest) = s"${r.method} ${r.uri}"

  /**
    * A simple logging TraceRoute
    *
    * {{{
    *   val route : Route = TraceRoute.log.wrap(myRoute)
    * }}}
    */
  object log extends TraceRoute with StrictLogging {
    override def onRequest(request: HttpRequest): Unit = {
      logger.info(s"onRequest($request)")
    }

    override def onResponse(request: HttpRequest, requestTime: Long, response: RouteResult): Unit = {
      val diff = System.currentTimeMillis - requestTime
      logger.info(s"${pretty(request)} took ${diff}ms to produce $response")
    }
  }

  /** @param requestCallback the request callback
    * @param responseCallback the response callback
    * @return a TraceRoute instance
    */
  def apply(requestCallback: HttpRequest => Unit, responseCallback: (HttpRequest, TimestampMillis, RouteResult) => Unit) = new TraceRoute {
    override def onRequest(request: HttpRequest): Unit = {
      requestCallback(request)
    }

    override def onResponse(request: HttpRequest, requestTime: TimestampMillis, response: RouteResult): Unit = {
      responseCallback(request, requestTime, response)
    }
  }

  /**
    * DSL for creating a TraceRoute handler
    *
    * @param callback the request callback
    * @return a TraceRequest instance which can be uses as a DSL to expose an 'onResponse' function in order to build a [[TraceRoute]]
    */
  def onRequest(callback: HttpRequest => Unit): TraceRequest = TraceRequest(callback)
}
