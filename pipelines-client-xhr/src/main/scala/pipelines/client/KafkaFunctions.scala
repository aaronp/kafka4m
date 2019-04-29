package pipelines.client

import endpoints.xhr.circe.JsonSchemaEntities
import io.circe.syntax._
import org.scalajs.dom
import org.scalajs.dom.raw.MessageEvent
import pipelines.kafka._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

trait KafkaFunctions extends HtmlUtils {

  object kafka extends JsonSchemaEntities with KafkaSchemas {

    def query(request: QueryRequest, isBinary: Boolean)(onNext: MessageEvent => Unit) = {
      val json: String = (UpdateFeedRequest(request): StreamingFeedRequest).asJson.noSpaces
      KafkaWSClient(initialHandshakeMessage = json, isBinary)(onNext)
    }

    def listTopics(): Unit = {
      val topics = KafkaQueryXhrClient.listTopics.listTopicsEndpoint
      topics.onComplete {
        case result => showAlert(s"ListTopics returned: ${result}")
      }
    }
  }

  import dom.window.console

  def query(clientElementElementId: String,
            groupElementId: String,
            topicElementId: String,
            filterTextElementId: String,
            filterExpressionIncludeMatchesId: String,
            offsetElementId: String,
            rateLimitElementId: String,
            strategyElementId: String,
            isBinaryStreamId: String)(onNext: MessageEvent => Unit) = {
    val limitOpt = Try(valueOf(rateLimitElementId).toInt).toOption.filter(_ > 0).map { limit =>
      Rate.perSecond(limit)
    }
    val streamStrategy = valueOf(strategyElementId) match {
      case "latest" => StreamStrategy.Latest
      case "all"    => StreamStrategy.All
      case other =>
        console.log(s"Expected latest or all but got '$other'")
        StreamStrategy.Latest
    }
    val isBinary = Try(valueOf(isBinaryStreamId).toBoolean).getOrElse(false)
    val request = QueryRequest(
      clientId = valueOfNonEmpty(clientElementElementId),
      groupId = valueOfNonEmpty(groupElementId),
      topic = valueOf(topicElementId),
      filterExpression = valueOf(filterTextElementId),
      filterExpressionIncludeMatches = Try(valueOf(filterExpressionIncludeMatchesId).toBoolean).getOrElse(false),
      fromOffset = Option(valueOf(offsetElementId)).filterNot(_.isEmpty),
      messageLimit = limitOpt,
      format = None,
      streamStrategy = streamStrategy
    )

    kafka.query(request, isBinary)(onNext)
  }
}
