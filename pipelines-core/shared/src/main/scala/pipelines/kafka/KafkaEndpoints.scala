package pipelines.kafka

import pipelines.core.BaseEndpoint

/**
  * Contains the core set of functionality for querying kafka
  */
trait KafkaEndpoints extends BaseEndpoint {

  object listTopics {

    def request: Request[Unit] = get(path / "kafka" / "topics")

    def response(implicit resp: JsonResponse[ListTopicsResponse]): Response[ListTopicsResponse] = jsonResponse[ListTopicsResponse](Option("returns the available topics"))

    def listTopicsEndpoint(implicit resp: JsonResponse[ListTopicsResponse]): Endpoint[Unit, ListTopicsResponse] = endpoint(request, response)
  }

  /**
    * The endpoint for creating a websocket which can consume (pull) data from us. We'll send the data and consume [[StreamingFeedRequest]] requests from the endpoint (updated queries, heartbeats or cancel)
    */
  object publish {
    type IsBinary = Boolean
    def request: Request[Option[IsBinary]] = get(path / "kafka" / "download" /? qs[Option[Boolean]]("binary"))
    def response: Response[Unit]           = emptyResponse(Option("The response is upgrade response to open a websocket"))

    val streamEndpoint: Endpoint[Option[IsBinary], Unit] = endpoint(request, response)
  }

  /**
    * The endpoint for creating a websocket which will consume data from the websocket and send StreamingRequest messages (heartbeats or a cancel request) to the connector
    */
  object consume {
    type IsBinary = Boolean
    def request: Request[Option[IsBinary]]               = get(path / "kafka" / "upload" /? qs[Option[Boolean]]("binary"))
    def response: Response[Unit]                         = emptyResponse(Option("The response is upgrade response to open a websocket"))
    val streamEndpoint: Endpoint[Option[IsBinary], Unit] = endpoint(request, response)
  }

  object query {
    type Topic  = String
    type Limit  = Long
    type Offset = Long

    type PullLatestRequest = (Topic, Offset, Limit)

    def request: Request[PullLatestRequest] = {
      get(path / "kafka" / "query" /? (qs[Topic]("topic") & qs[Offset]("offset") & qs[Limit]("limit")))
    }

    def response(implicit resp: JsonResponse[PullLatestResponse]): Response[PullLatestResponse] = jsonResponse[PullLatestResponse](Option("returns data from the topic"))

    def pullEndpoint(implicit resp: JsonResponse[PullLatestResponse]): Endpoint[PullLatestRequest, PullLatestResponse] = endpoint(request, response)
  }
}
