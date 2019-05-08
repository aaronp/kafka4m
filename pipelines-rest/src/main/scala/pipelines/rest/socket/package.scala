package pipelines.rest

import akka.http.scaladsl.model.ws.TextMessage
import pipelines.core.{Heartbeat, StreamingRequest}

package object socket {
  val heartBeatTextMsg: TextMessage.Strict = {
    import io.circe.syntax._
    val heartbeatJson = (Heartbeat: StreamingRequest).asJson.noSpaces
    TextMessage(heartbeatJson)
  }

}
