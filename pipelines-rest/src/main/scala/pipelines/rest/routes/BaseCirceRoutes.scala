package pipelines.rest.routes

import com.typesafe.scalalogging.StrictLogging
import endpoints.akkahttp.server
import endpoints.akkahttp.server.JsonSchemaEntities
import pipelines.core.GenericMessageResult

trait BaseCirceRoutes extends server.Endpoints with JsonSchemaEntities with endpoints.circe.JsonSchemas with StrictLogging {

  implicit def responseSchema: JsonSchema[GenericMessageResult] = JsonSchema(implicitly, implicitly)

}
