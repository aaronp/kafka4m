package kafkaquery.client.jvm

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import endpoints.akkahttp.client.{AkkaHttpRequestExecutor, Endpoints, EndpointsSettings, JsonEntitiesFromCodec}
import endpoints.algebra.BasicAuthentication
import kafkaquery.endpoints.UserEndpoints

import scala.concurrent.ExecutionContext

abstract class BaseClient(settings: EndpointsSettings)(implicit ec: ExecutionContext, mat: Materializer)
    extends Endpoints(settings) with BasicAuthentication with JsonEntitiesFromCodec //with circe.JsonEntitiesFromCodec {}

//abstract class EsaClient(settings: EndpointsSettings)(implicit ec: ExecutionContext, mat: Materializer) extends BaseClient(settings) with UserEndpoints {
//
//  val userLogin = login
//}

object EsaClient {

  def apply(host: String, port: Int)(implicit mat: ActorMaterializer) = {
    implicit val system: ActorSystem = mat.system
    implicit val ec                  = system.dispatcher

    //new EsaClient(EndpointsSettings(AkkaHttpRequestExecutor.cachedHostConnectionPool(host, port)))

    ???
  }
}
