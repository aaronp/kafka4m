package kafkaquery.client.jvm

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.stream.{ActorMaterializer, Materializer}
import endpoints.akkahttp.client.{AkkaHttpRequestExecutor, Endpoints, EndpointsSettings, JsonEntitiesFromCodec}
import endpoints.algebra.BasicAuthentication
import kafkaquery.kafka.KafkaEndpoints

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

//BasicAuthentication with
abstract class KafkaQueryClient(settings: EndpointsSettings)(implicit ec: ExecutionContext, mat: Materializer) extends Endpoints(settings) with KafkaEndpoints { //with endpoints.algebra.circe.JsonEntitiesFromCodec with JsonEntitiesFromCodec {

}

object KafkaQueryClient {

  def apply(host: String, port: Int, toStrictTimeout: FiniteDuration)(implicit mat: ActorMaterializer): KafkaQueryClient = {
    implicit val system: ActorSystem = mat.system
    implicit val ec                  = system.dispatcher
    val settings                     = settingsFor(host, port, toStrictTimeout)
    //new KafkaQueryClient(settings)
    ???
  }

  def settingsFor(host: String, port: Int, toStrictTimeout: FiniteDuration)(implicit mat: ActorMaterializer) = {
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
