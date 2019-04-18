package pipelines.kafka

import pipelines.core.BaseEndpoint

/**
  * Contains the core set of functionality for querying kafka
  */
trait KafkaEndpoints extends BaseEndpoint {

  def kafkaEndpoints(implicit resp1: JsonResponse[ListTopicsResponse], resp2: JsonResponse[PullLatestResponse]) = List(
    listTopics.listTopicsEndpoint,
    query.pullEndpoint,
    stream.streamEndpoint
  )

  object listTopics {

    def request: Request[Unit] = get(path / "kafka" / "topics")

    def response(implicit resp: JsonResponse[ListTopicsResponse]): Response[ListTopicsResponse] = jsonResponse[ListTopicsResponse](Option("returns the available topics"))

    def listTopicsEndpoint(implicit resp: JsonResponse[ListTopicsResponse]): Endpoint[Unit, ListTopicsResponse] = endpoint(request, response)
  }

  object stream {
    type IsBinary = Boolean
    def request: Request[Option[IsBinary]] = get(path / "kafka" / "stream" /? qs[Option[Boolean]]("binary"))
    def response: Response[Unit]           = emptyResponse(Option("The response is upgrade response to open a websocket"))

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
