package pipelines.kafka

import io.circe.generic.auto._
import pipelines.core.BaseEndpoint

/**
  * Contains the core set of functionality for querying kafka
  */
trait KafkaEndpoints extends BaseEndpoint {

  def kafkaEndpoints = List(
    listTopics.listTopicsEndpoint,
    pullLatest.pullEndpoint,
    stream.streamEndpoint
  )

  object listTopics {

    def request: Request[Unit] = get(path / "kafka" / "topics")

    def response: Response[ListTopicsResponse] = jsonResponse[ListTopicsResponse](Option("returns the available topics"))

    implicit lazy val requestSchema : JsonSchema[ListTopicsResponse] = JsonSchema(implicitly, implicitly)

    val listTopicsEndpoint: Endpoint[Unit, ListTopicsResponse] = endpoint(request, response)
  }

  object stream {
    type IsBinary = Boolean
    def request: Request[Option[IsBinary]] = get(path / "kafka" / "stream" /? qs[Option[Boolean]]("binary"))
    def response: Response[Unit] = emptyResponse(Option("The response is upgrade response to open a websocket"))

    val streamEndpoint: Endpoint[Option[IsBinary], Unit] = endpoint(request, response)
  }

  object pullLatest {
    type Topic  = String
    type Limit  = Long
    type Offset = Long

    type PullLatestRequest = (Topic, Offset, Limit)

    def request: Request[PullLatestRequest] = {
      get(path / "kafka" / "pull" /? (qs[Topic]("topic") & qs[Offset]("offset") & qs[Limit]("limit")))
    }

    def response: Response[PullLatestResponse] = jsonResponse[PullLatestResponse](Option("returns data from the topic"))

    implicit lazy val requestSchema : JsonSchema[PullLatestResponse] = JsonSchema(implicitly, implicitly)

    val pullEndpoint: Endpoint[PullLatestRequest, PullLatestResponse] = endpoint(request, response)
  }
  object associateSchema {
    type Topic  = String
    type Limit  = Long
    type Offset = Long

    type PullLatestRequest = (Topic, Offset, Limit)

    def request: Request[PullLatestRequest] = {
      get(path / "kafka" / "pull" /? (qs[Topic]("topic") & qs[Offset]("offset") & qs[Limit]("limit")))
    }

    def response: Response[PullLatestResponse] = jsonResponse[PullLatestResponse](Option("returns data from the topic"))

    implicit lazy val `JsonSchema[PullLatestResponse]` : JsonSchema[PullLatestResponse] = JsonSchema(implicitly, implicitly)

    val pullEndpoint: Endpoint[PullLatestRequest, PullLatestResponse] = endpoint(request, response)
  }

}
