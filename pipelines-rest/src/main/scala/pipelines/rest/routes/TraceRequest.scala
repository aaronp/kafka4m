package pipelines.rest.routes

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.{RouteResult, _}
import akka.http.scaladsl.server.directives.BasicDirectives.{extractRequestContext, mapRouteResult}
import com.typesafe.scalalogging.StrictLogging

/**
  * Just a noddy interface for building routes -- a way to introduce side-effects for requests.
  *
  *
  * You can use 'onResponse' to build a DSL for wrapping both requests and responses, or just 'wrap' if you're
  * only concerned about requests:
  *
  * {{{
  *      TraceRequest { request =>
  *         logger.info(s"Got $request")
  *       }.wrap {
  *         get {
  *           path("foo") {
  *             complete("ok")
  *           }
  *         }
  *       }
  * }}}
  *
  * Or more likely:
  *
  * {{{
  * val myAppRoutes : Route = ???
  * val logging = TraceRequest { request =>
  *   logger.info(s"Got $request")
  * }
  * val routes : Route = logging.wrap(myAppRoutes)
  *
  * }}}
  *
  * Not that logging is a great example, as there are already logging directives
  */
trait TraceRequest {

  /** A callback handler which will get invoked when a request is encountered
    *
    * @param callback the request callback
    */
  def onRequest(callback: HttpRequest): Unit

//  def onResponse(callback: (HttpRequest, TimestampMillis, RouteResult) => Unit): TraceRoute = TraceRoute(onRequest, callback)
  /** @param callback the response callback
    * @return a TraceRoute which exposes a 'wrap' directive similar to this, but one which is invoked on responses as well
    */
  def onResponse(callback: PartialFunction[(HttpRequest, TimestampMillis, RouteResult), Unit]): TraceRoute = {
    TraceRoute(onRequest, {
      case tuple if callback.isDefinedAt(tuple) => callback(tuple)
      case _                                    =>
    })
  }

  /** @return a directive which can wrap routes by invoking this 'onRequest' callback
    */
  final def wrap: Directive0 = extractRequestContext.tmap { ctxt =>
    onRequest(ctxt._1.request)
  }
}

object TraceRequest {
  def apply(callback: HttpRequest => Unit) = new TraceRequest {
    override def onRequest(request: HttpRequest): Unit = {
      callback(request)
    }
  }
}
