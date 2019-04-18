package pipelines.client.jvm

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.stream.{ActorMaterializer, Materializer}
import endpoints.akkahttp.client.{AkkaHttpRequestExecutor, Endpoints, EndpointsSettings, JsonEntitiesFromCodec}
import pipelines.kafka.KafkaEndpoints

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

//BasicAuthentication with
abstract class PipelinesClient(settings: EndpointsSettings)(implicit ec: ExecutionContext, mat: Materializer) extends Endpoints(settings) with JsonEntitiesFromCodec
//    with endpoints.algebra.circe.JsonEntitiesFromCodec
//    with KafkaEndpoints

object PipelinesClient {

  def apply(host: String, port: Int, toStrictTimeout: FiniteDuration)(implicit mat: ActorMaterializer): PipelinesClient = {
    implicit val system: ActorSystem = mat.system
    implicit val ec                  = system.dispatcher
    val settings                     = settingsFor(host, port, toStrictTimeout)
    //new PipelinesClient(settings)
    ???
  }

  def settingsFor(host: String, port: Int, toStrictTimeout: FiniteDuration)(implicit mat: ActorMaterializer): EndpointsSettings = {
    implicit val system: ActorSystem = mat.system
    implicit val ec                  = system.dispatcher

    val requestExecutor = AkkaHttpRequestExecutor.cachedHostConnectionPool(host, port)
    EndpointsSettings(
      requestExecutor = requestExecutor,
      baseUri = Uri("/"),
      toStrictTimeout = toStrictTimeout
    )
  }
}
