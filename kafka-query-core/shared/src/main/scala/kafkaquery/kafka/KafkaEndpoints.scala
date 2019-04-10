package kafkaquery.kafka

import io.circe.generic.auto._
import kafkaquery.core.BaseEndpoint

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

    def request: Request[Unit] = {
      get(path / "kafka" / "topics")
    }

    def response: Response[ListTopicsResponse] = jsonResponse[ListTopicsResponse](Option("returns the available topics"))

    implicit lazy val `JsonSchema[ListTopicsResponse]` : JsonSchema[ListTopicsResponse] = JsonSchema(implicitly, implicitly)

    val listTopicsEndpoint: Endpoint[Unit, ListTopicsResponse] = endpoint(request, response)
  }

  object stream {

    def request: Request[StreamRequest] = {
      val streamRequest = jsonRequest[StreamRequest](Option("request which will open a reactive stream web socket"))
      post(path / "kafka" / "stream", streamRequest)
    }
    def response = emptyResponse(Option("The response is upgrade response to open a websocket"))

    implicit lazy val `JsonSchema[ListTopicsResponse]` : JsonSchema[Unit]     = JsonSchema(implicitly, implicitly)
    implicit lazy val `JsonSchema[StreamRequest]` : JsonSchema[StreamRequest] = JsonSchema(implicitly, implicitly)

    val streamEndpoint: Endpoint[StreamRequest, Unit] = endpoint(request, response)
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

    implicit lazy val `JsonSchema[PullLatestResponse]` : JsonSchema[PullLatestResponse] = JsonSchema(implicitly, implicitly)

    val pullEndpoint: Endpoint[PullLatestRequest, PullLatestResponse] = endpoint(request, response)
  }

}
