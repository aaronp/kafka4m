package pipelines.client

import pipelines.kafka.{Heartbeat, StreamingFeedRequest}
import org.scalajs.dom
import org.scalajs.dom.raw.{MessageEvent, WebSocket}

import scala.concurrent.duration._

object KafkaWSClient {

  def apply(initialHandshakeMessage: String, isBinary: Boolean, hbFrequency: FiniteDuration = 3.seconds)(onNext: MessageEvent => Unit): WebSocket = {
    val loc = dom.window.location
    import dom.window.console

    console.log(s"host:${loc.host}")
    console.log(s"hostname:${loc.hostname}")
    console.log(s"href:${loc.href}")
    console.log(s"pathname:${loc.pathname}")
    console.log(s"protocol:${loc.protocol}")
    console.log(s"search:${loc.search}")
    console.log(s"origin:${loc.origin}")

    val url = s"wss://${loc.host}/kafka/stream?binary=${isBinary}"
    console.log(s"connecting to ${url}")
    val socket: WebSocket = new WebSocket(url)

    var heartbeatHandle = Option.empty[Int]
    def startHeartbeat() = {
      import io.circe.syntax._
      val heartbeat = (Heartbeat: StreamingFeedRequest).asJson.noSpaces
      dom.window.setInterval(() => socket.send(heartbeat), hbFrequency.toMillis)
    }
    def stopHeartbeat() = {
      heartbeatHandle.foreach { id =>
        dom.window.clearInterval(id)
        heartbeatHandle = None
      }
    }

    socket.onclose = { evt =>
      console.log(s"onClose ${evt.reason}")
      stopHeartbeat()
    }
    socket.onerror = { evt =>
      stopHeartbeat()
      console.log(s"onError ${evt}")
    }
    socket.onmessage = { msg =>
      console.log(s"onMessage ${msg}")
      onNext(msg)
    }
    socket.onopen = { msg =>
      console.log(s"onopen ${msg}")
      socket.send(initialHandshakeMessage)
      heartbeatHandle = Option(startHeartbeat)
    }

    socket
  }

}
