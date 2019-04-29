package pipelines.kafka

import pipelines.admin.GenerateServerCertRequest

trait KafkaSchemas extends endpoints.circe.JsonSchemas {

  implicit def generateServerCertRequestSchema: JsonSchema[GenerateServerCertRequest] = JsonSchema(implicitly, implicitly)
  implicit def listTopicsResponseSchema: JsonSchema[ListTopicsResponse]               = JsonSchema(implicitly, implicitly)
  implicit def pullLatestResponseSchema: JsonSchema[PullLatestResponse]               = JsonSchema(implicitly, implicitly)

}
