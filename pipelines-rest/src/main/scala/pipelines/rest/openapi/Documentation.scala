package pipelines.rest.openapi

import endpoints.openapi
import endpoints.openapi.model.{Info, OpenApi}
import io.circe.Json
import pipelines.admin._
import pipelines.core.{AnyType, CreateAvroSource, CreateSourceRequest, Enrichment, GenericMessageResult}
import pipelines.data.{DataRegistryResponse, SourceCreatedResponse}
import pipelines.kafka.{KafkaEndpoints, KafkaSupportEndpoints, ListTopicsResponse, PartitionData, PublishMessage}
import pipelines.stream.{ListSourceResponse, PeekResponse, RegisteredSource, StreamEndpoints}
import pipelines.users.{CreateUserRequest, CreateUserResponse, LoginRequest, LoginResponse, UserEndpoints}

object OpenApiEncoder extends endpoints.openapi.model.OpenApiSchemas with endpoints.circe.JsonSchemas {
  implicit def requestSchema: JsonSchema[GenerateServerCertRequest] = JsonSchema(implicitly, implicitly)
}

/**
  * Generates OpenAPI documentation for the endpoints described in the `CounterEndpoints` trait.
  */
object Documentation           //
    extends openapi.Endpoints  //
    with CirceAdapter          //
    with UserEndpoints         //
    with KafkaEndpoints        //
    with StreamEndpoints       //
    with KafkaSupportEndpoints //
    with AdminEndpoints        //
    with openapi.JsonSchemaEntities {

  import OpenApiEncoder.JsonSchema._

  val genericResp: Documentation.DocumentedJsonSchema = document(GenericMessageResult("a response message"))

  def userEndpointDocs = {
    List(
      loginEndpoint(                                                                        //
                    document(LoginRequest("username", "password")),                         //
                    document(LoginResponse(true, Option("jwtToken"), Option("redirectTo"))) //
      ),                                                                                    //
      createUserEndpoint(
        document(CreateUserRequest("username", "em@ail.com", "password")), //
        document(CreateUserResponse(true, Option("jwtToken")))             //
      )
    )
  }
  def adminEndpointDocs: List[Documentation.DocumentedEndpoint] = {
    List(
      generate.generateEndpoint(                                                    //
                                document(GenerateServerCertRequest("saveToPath")),  //
                                document(GenerateServerCertResponse("certificate")) //
      ),
      updatecert.updateEndpoint(                                                                  //
                                document(UpdateServerCertRequest("certificate", "save/to/path")), //
                                genericResp),
      seed.seedEndpoint(document(SetJWTSeedRequest("seed")), genericResp)
    )
  }

  def kafkaEndpointsDocs: List[Documentation.DocumentedEndpoint] = List(
    listTopics.listTopicsEndpoint(document(ListTopicsResponse(Map("/some/topic" -> Seq(PartitionData(1, "leader")))))),
    query.pullEndpoint(document(("topic", 1, 2))),
    publish.streamEndpoint,
    consume.streamEndpoint
  )
  def kafkaSupportEndpointsDocs: List[Documentation.DocumentedEndpoint] = {
    List(
      publishSupport.publishEndpoint(document(PublishMessage("topic", "key", "data")), genericResp),
      config.configEndpoint(genericResp)
    )
  }

  def streamEndpoints: List[Documentation.DocumentedEndpoint] = {
    val anyJson               = Json.obj("any" -> Json.fromString("value"))
    val enrichmet: Enrichment = Enrichment.MapType(AnyType("string"))
    val dataRegistryResponse  = document(SourceCreatedResponse("someSource", AnyType("string")): DataRegistryResponse)
    List(
      list.listSourcesEndpoint(document(ListSourceResponse(Seq(RegisteredSource("someSource", "string"))))),
      websocketConsume.consumeEndpoint(dataRegistryResponse),
      websocketPublish.publishEndpoint(document(Option("optionalSourceId"))),
      peek.peekEndpoint(document(PeekResponse(anyJson))),
      copy.copyEndpoint(document(enrichmet), dataRegistryResponse),
      update.updateEndpoint(document(enrichmet), dataRegistryResponse),
      create.createEndpoint(document(CreateAvroSource("schema"): CreateSourceRequest), dataRegistryResponse),
      push.pushEndpoint(document(anyJson), document(true))
    )
  }

  def documentedEndpoints: List[Documentation.DocumentedEndpoint] = {
    kafkaSupportEndpointsDocs ++ streamEndpoints ++ kafkaEndpointsDocs ++ userEndpointDocs ++ adminEndpointDocs
  }

  lazy val api: OpenApi = openApi(
    Info(title = "OpenApi schema", version = "1.0.0")
  )(documentedEndpoints: _*)

}
