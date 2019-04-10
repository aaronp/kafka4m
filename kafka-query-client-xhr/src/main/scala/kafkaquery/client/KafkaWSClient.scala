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
  }

}
