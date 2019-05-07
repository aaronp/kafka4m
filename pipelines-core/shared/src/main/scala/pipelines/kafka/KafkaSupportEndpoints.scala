package pipelines.kafka

import pipelines.core.{BaseEndpoint, GenericMessageResult}

trait KafkaSupportEndpoints extends BaseEndpoint {

  object publishSupport {

    def request(implicit req: JsonRequest[PublishMessage]): Request[PublishMessage] = {
      val publishRequest = jsonRequest[PublishMessage](Option("publishes a message to a topic"))
      post(path / "kafka" / "support" / "publish", publishRequest)
    }

    def publishEndpoint(implicit req: JsonRequest[PublishMessage], resp: JsonResponse[GenericMessageResult]): Endpoint[PublishMessage, GenericMessageResult] =
      endpoint(request, genericMessageResponse)
  }

  object config {
    def request: Request[Option[String]]                                                                                  = get(path / "kafka" / "support" / "config" /? qs[Option[String]]("path", Option("The path of the configuration key to show")))
    def configEndpoint(implicit resp: JsonResponse[GenericMessageResult]): Endpoint[Option[String], GenericMessageResult] = endpoint(request, genericMessageResponse)
  }

}
