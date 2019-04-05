package kafkaquery.rest

import endpoints.openapi
import endpoints.openapi.model.{Info, OpenApi}
import kafkaquery.admin.AdminEndpoints
import kafkaquery.kafka.KafkaEndpoints
import kafkaquery.users.UserEndpoints

/**
  * Generates OpenAPI documentation for the endpoints described in the `CounterEndpoints` trait.
  */
object Documentation //
    extends openapi.Endpoints       //
    with openapi.JsonSchemaEntities //
//    with UserEndpoints  //
//    with KafkaEndpoints //
//    with AdminEndpoints //
    {

  val documentedEndpoints: List[Documentation.DocumentedEndpoint] = {
//    userEndpoints ++ adminEndpoints
    Nil
  }

  val api: OpenApi = openApi(
    Info(title = "OpenApi schema", version = "1.0.0")
  )(documentedEndpoints: _*)

}
