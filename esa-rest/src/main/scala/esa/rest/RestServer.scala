package esa.rest
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.io.StdIn

object RestServer {

  def main(args: Array[String]) : Unit = {

    implicit val system           = ActorSystem("my-system")
    implicit val materializer     = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val staticFiles = StaticFileServer.dev

    val route: Route = EsaRoutes(staticFiles)

    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
