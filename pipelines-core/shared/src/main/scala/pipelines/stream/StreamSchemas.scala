package pipelines.stream

import io.circe.Json
import pipelines.core.CreateSourceRequest
import pipelines.data.DataRegistryResponse

trait StreamSchemas extends endpoints.circe.JsonSchemas {

  implicit def CreateSourceRequestSchema: JsonSchema[CreateSourceRequest]   = JsonSchema(implicitly, implicitly)
  implicit def ListSourceResponseSchema: JsonSchema[ListSourceResponse]     = JsonSchema(implicitly, implicitly)
  implicit def PeekResponseSchema: JsonSchema[PeekResponse]                 = JsonSchema(implicitly, implicitly)
  implicit def DataRegistryResponseSchema: JsonSchema[DataRegistryResponse] = JsonSchema(implicitly, implicitly)
  implicit def CirceJsonSchema: JsonSchema[Json]                            = JsonSchema(implicitly, implicitly)

}
