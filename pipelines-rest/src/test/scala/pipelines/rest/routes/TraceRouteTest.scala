package pipelines.rest.routes

import java.util.concurrent.atomic._

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

class TraceRouteTest extends WordSpec with Matchers with ScalatestRouteTest with ScalaFutures {

  "TraceRoute" should {
    "Invoke the callbacks for the correct route" in {

      val fastRequests  = new AtomicInteger(0)
      val fastResponses = new AtomicInteger(0)
      val slowRequests  = new AtomicInteger(0)
      val slowResponses = new AtomicInteger(0)
      object totals extends TraceRequest {
        var total = 0
        override def onRequest(callback: HttpRequest): Unit = {
          total = total + 1
        }
      }
      val trace = TraceRoute
        .onRequest { request =>
          val path = request.uri.toString
          println(path)
          if (path.contains("fast")) {
            fastRequests.incrementAndGet
          } else if (path.contains("slow")) {
            slowRequests.incrementAndGet
          }
        }
        .onResponse {
          case (request, _, _) if request.uri.toString.contains("fast") =>
            fastResponses.incrementAndGet
          case (request, _, _) if request.uri.toString.contains("slow") =>
            slowResponses.incrementAndGet
        }

      val routes = Route.seal(totals.wrap(trace.wrap {
        get {
          path("fast") {
            complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "You hit the first route!"))
          }
        } ~
          get {
            path("slow") {
              complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "You hit the slow route!"))
            }
          }
      }))

      Get("/fast") ~> routes ~> check {
        response.status.isSuccess() shouldBe true
        val body = entityAs[String]
        body shouldBe "You hit the first route!"
      }
      totals.total shouldBe 1
      fastRequests.get shouldBe 1
      fastResponses.get shouldBe 1
      slowRequests.get shouldBe 0
      slowResponses.get shouldBe 0

      Get("/slow") ~> routes ~> check {
        response.status.isSuccess() shouldBe true
        val body = entityAs[String]
        body shouldBe "You hit the slow route!"
      }
      totals.total shouldBe 2
      fastRequests.get shouldBe 1
      fastResponses.get shouldBe 1
      slowRequests.get shouldBe 1
      slowResponses.get shouldBe 1

      Get("/meh") ~> routes ~> check {
        response.status.intValue shouldBe 404
      }
      totals.total shouldBe 3
      fastRequests.get shouldBe 1
      fastResponses.get shouldBe 1
      slowRequests.get shouldBe 1
      slowResponses.get shouldBe 1
    }
  }
}
