package kafkaquery.rest

import endpoints.openapi
import endpoints.openapi.model.{Info, OpenApi}

/**
  * Generates OpenAPI documentation for the endpoints described in the `CounterEndpoints` trait.
  */
object Documentation //
    extends openapi.Endpoints
//    with UserEndpoints      //
//    with KafkaEndpoints        //
//    with KafkaSupportEndpoints //
//    with AdminEndpoints        //
    with openapi.JsonSchemaEntities {

  val documentedEndpoints: List[Documentation.DocumentedEndpoint] = {
//    userEndpoints ++ adminEndpoints ++ kafkaEndpoints ++ kafkaSupportEndpoints
    Nil
  }

  val api: OpenApi = openApi(
    Info(title = "OpenApi schema", version = "1.0.0")
  )(documentedEndpoints: _*)

}
