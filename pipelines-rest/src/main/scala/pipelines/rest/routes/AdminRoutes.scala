package pipelines.rest.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.StrictLogging
import pipelines.admin._
import pipelines.core.GenericMessageResult

import scala.util.control.NonFatal

object AdminRoutes extends AdminEndpoints with BaseCirceRoutes {
  implicit def genericResp: JsonSchema[GenericMessageResult] = JsonSchema(implicitly, implicitly)

  val generateCertRoute: Route = {
    implicit def requestSchema: JsonSchema[GenerateServerCertRequest]   = JsonSchema(implicitly, implicitly)
    implicit def responseSchema: JsonSchema[GenerateServerCertResponse] = JsonSchema(implicitly, implicitly)

    generate.generateEndpoint.implementedBy {
      case GenerateServerCertRequest(saveToPath) =>
        try {

          ???
        } catch {
          case NonFatal(e) =>
            ???
        }
    }
  }

  val updateCertRoute = {
    implicit def requestSchema: JsonSchema[UpdateServerCertRequest] = JsonSchema(implicitly, implicitly)
//    implicit def responseSchema: JsonSchema[GenerateServerCertResponse] = JsonSchema(implicitly, implicitly)

    updatecert.updateEndpoint.implementedBy { request =>
      logger.info(s"${request.certificate} for ${request.saveToPath}}")

      ???
    }
  }

  val setSeedRoute = {

    implicit def requestSchema: JsonSchema[SetJWTSeedRequest] = JsonSchema(implicitly, implicitly)
    seed.seedEndpoint.implementedBy { request =>
      ???
    }
  }

  def routes = generateCertRoute ~ updateCertRoute ~ setSeedRoute
}
