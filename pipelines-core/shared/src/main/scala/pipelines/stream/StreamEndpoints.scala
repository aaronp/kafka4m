package pipelines.stream

import io.circe.Json
import pipelines.core.{BaseEndpoint, CreateSourceRequest, Enrichment}
import pipelines.data.DataRegistryResponse

/**
  * $ GET /source # list registered sources
  * $ GET /source/{id}/socket # upgrade a GET request to connect a websocket to the source for the specified id
  * $ GET /source/{id}/peek # get the latest value at the given id
  *
  * $ POST /source/{id}/copy # enrich the source at 'id' by indexing it
  * $ POST /source/{id}/update # enrich the source at 'id' by indexing it
  *
  * $ GET /source/create/socket?id # creates a source from the data sent from an upgraded web socket
  * $ POST /source/create/kafka?id # creates a source from a kafka
  * $ POST /source/create/upload?id # creates a source from a multi-part upload
  *
  * $ POST /sink/{id}/kafka # create a kafka sink
  */
trait StreamEndpoints extends BaseEndpoint {

  type IsBinary = Boolean

  /** list registered sources:
    *
    * GET /source
    */
  object list {
    def request: Request[Unit] = get(path / "source")

    def response(resp: JsonResponse[ListSourceResponse]): Response[ListSourceResponse] = jsonResponse[ListSourceResponse](Option("The known sources"))(resp)

    def listSourcesEndpoint(resp: JsonResponse[ListSourceResponse]): Endpoint[Unit, ListSourceResponse] = endpoint(request, response(resp))
  }

  /** connect a web socket to a source
    * GET /source/{id}/consume?binary=true
    */
  object websocketConsume {
    def request: Request[(String, Option[IsBinary])] = get(path / "source" / segment[String]("id", Option("a unique stream id")) / "consume" /? qs[Option[Boolean]]("binary"))
    def response(implicit resp: JsonResponse[DataRegistryResponse]): Response[DataRegistryResponse] = {
      jsonResponse(Option("The response is upgrade response to open a websocket"))(resp)
    }

    def consumeEndpoint(implicit resp: JsonResponse[DataRegistryResponse]): Endpoint[(String, Option[IsBinary]), Unit] = endpoint(request, response(resp))
  }

  /** connect a web socket to a source
    * GET /source/{id}/peek
    */
  object peek {
    def request: Request[String]                                                    = get(path / "source" / segment[String]("id", Option("a unique stream id")) / "peek")
    def response(implicit resp: JsonResponse[PeekResponse]): Response[PeekResponse] = jsonResponse[PeekResponse](Option("the first data returned from a source"))(resp)

    def peekEndpoint(implicit resp: JsonResponse[PeekResponse]): Endpoint[String, PeekResponse] = endpoint(request, response)
  }

  /** produce a new filtered endpoint from a source
    * POST /source/{id}/copy
    */
  object copy {
    def request(implicit req: JsonRequest[Enrichment]): Request[(String, Enrichment)] =
      post(
        path / "source" / segment[String]("id", Option("a unique stream id")) / "copy",
        jsonRequest[Enrichment](Option("type of enrichment to add to the source"))(req)
      )
    def response(implicit resp: JsonResponse[DataRegistryResponse]): Response[DataRegistryResponse] = {
      jsonResponse[DataRegistryResponse](Option("the result of enriching the source w/ the specified enrichment"))(resp)
    }

    def copyEndpoint(implicit req: JsonRequest[Enrichment], resp: JsonResponse[DataRegistryResponse]) = endpoint(request(req), response)
  }

  /** update a source
    * POST /source/{id}/update
    */
  object update {
    def request(implicit req: JsonRequest[Enrichment]) =
      post(
        path / "source" / segment[String]("id", Option("a unique stream id")) / "update",
        jsonRequest[Enrichment](
          Option(
            "type of enrichment with which to update the source. If the source registered at the given ID isn't already a source of the provided type then an error is returned"))(
          req)
      )
    def response(implicit resp: JsonResponse[DataRegistryResponse]): Response[DataRegistryResponse] =
      jsonResponse[DataRegistryResponse](Option("the first data returned from a source"))(resp)

    def updateEndpoint(implicit req: JsonRequest[Enrichment], resp: JsonResponse[DataRegistryResponse]) = endpoint(request(req), response)
  }

  /**
    * create a route which will connect to an external system (e.g. kafka), accept [[push.pushEndpoint]] calls, etc
    *
    * POST /source/create
    */
  object create {
    def request(implicit req: JsonRequest[CreateSourceRequest]): Request[(Option[String], CreateSourceRequest)] =
      post(path / "source" / "create" /? qs[Option[String]]("id"), jsonRequest[CreateSourceRequest](Option("Creates a new source"))(req))
    def response(implicit resp: JsonResponse[DataRegistryResponse]): Response[DataRegistryResponse] =
      jsonResponse[DataRegistryResponse](Option("the create response"))(resp)

    def createEndpoint(implicit req: JsonRequest[CreateSourceRequest],
                       resp: JsonResponse[DataRegistryResponse]): Endpoint[(Option[String], CreateSourceRequest), DataRegistryResponse] = endpoint(request(req), response)
  }

  /**
    * POST /source/push
    */
  object push {
    def request(implicit req: JsonRequest[Json]): Request[(String, Json)] =
      post(path / "source" / "push" / segment[String]("id"), jsonRequest[Json](Option("push some data to a source"))(req))
    def response(implicit resp: JsonResponse[Boolean]): Response[Boolean] =
      jsonResponse[Boolean](Option("the push response"))(resp)

    def pushEndpoint(implicit req: JsonRequest[Json], resp: JsonResponse[Boolean]): Endpoint[(String, Json), Boolean] = endpoint(request(req), response)
  }

  /**
    * $ GET /source/create/publish?id=XXX&binary=true # creates a source from the data sent from an upgraded web socket
    */
  object websocketPublish {
    def request: Request[(Option[String], Option[IsBinary])] = get(path / "source" / "create" / "publish" /? (qs[Option[String]]("id") & qs[Option[IsBinary]]("binary")))
    def response: Response[Unit]                             = emptyResponse(Option("The response is upgrade response to open a websocket"))

    val publishEndpoint: Endpoint[(Option[String], Option[IsBinary]), Unit] = endpoint(
      request,
      response,
      description = Option("""Creates a source from the data sent from an upgraded web socket.
        |If the 'id' is specified, it will push to an existing source, presumably created from the 'create' endpoint.
        |If the 'id' exists but isn't a pushable source then this will error.
        |If the 'id' is specified but does not exist then a source with the given ID will be created.
        |If the 'id' is not specified then a source with a unique ID will be created.
        |
        |In the cases where a new source is created, a persistent sink will also be created and immediately connected as well - otherwise the 
        |data could just go to '/dev/null'.
        |
      """.stripMargin)
    )
  }

}
