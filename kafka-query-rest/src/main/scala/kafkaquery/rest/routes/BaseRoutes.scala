package kafkaquery.rest.routes

import endpoints.akkahttp.server
import endpoints.akkahttp.server.JsonSchemaEntities

trait BaseRoutes extends server.Endpoints with JsonSchemaEntities
