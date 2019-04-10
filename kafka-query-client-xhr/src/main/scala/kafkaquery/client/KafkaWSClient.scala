package kafkaquery.client

import org.scalajs.dom
import org.scalajs.dom.raw.WebSocket

object KafkaWSClient {

  def apply() = {
    val loc = dom.window.location

    dom.window.console.log(s"host:${loc.host}")
    dom.window.console.log(s"hostname:${loc.hostname}")
    dom.window.console.log(s"href:${loc.href}")
    dom.window.console.log(s"pathname:${loc.pathname}")
    dom.window.console.log(s"protocol:${loc.protocol}")
    dom.window.console.log(s"search:${loc.search}")
    dom.window.console.log(s"origin:${loc.origin}")

    val socket: WebSocket = new WebSocket(s"wss://${loc.hostname}/kafka/connect")
    socket.onclose = { evt =>
      dom.window.console.log(s"onClose ${evt.reason}")
    }
    socket.onerror = { evt =>
      dom.window.console.log(s"onError ${evt}")
    }
    socket.onmessage = { msg =>
      dom.window.console.log(s"onMessage ${msg}")
    }
    socket.onopen = { msg =>
      dom.window.console.log(s"onopen ${msg}")
    }

    def send(id: Int) = {
      socket.send(s"value:$id")
    }

    (0 to 100).map(_ + 1).foreach { i =>
      val after = i * 1000
      dom.window.setTimeout(() => send(i), after)
    }

  }

}
