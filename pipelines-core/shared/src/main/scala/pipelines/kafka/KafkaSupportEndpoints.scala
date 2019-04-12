package pipelines.kafka

import io.circe.generic.auto._
import pipelines.core.{BaseEndpoint, GenericMessageResult}

trait KafkaSupportEndpoints extends BaseEndpoint {

  def kafkaSupportEndpoints = List(
    publish.publishEndpoint,
    config.configEndpoint
  )

  object publish {

    def request: Request[PublishMessage] = {
      val publishRequest = jsonRequest[PublishMessage](Option("publishes a message to a topic"))
      post(path / "kafka" / "support" / "publish", publishRequest)
    }

    implicit lazy val `JsonSchema[PublishMessage]` : JsonSchema[PublishMessage] = JsonSchema(implicitly, implicitly)
    val publishEndpoint: Endpoint[PublishMessage, GenericMessageResult]         = endpoint(request, genericMessageResponse)
  }

  object config {
    def request: Request[Option[String]]                               = get(path / "kafka" / "support" / "config" /? qs[Option[String]]("path", Option("The path of the configuration key to show")))
    val configEndpoint: Endpoint[Option[String], GenericMessageResult] = endpoint(request, genericMessageResponse)
  }

}
