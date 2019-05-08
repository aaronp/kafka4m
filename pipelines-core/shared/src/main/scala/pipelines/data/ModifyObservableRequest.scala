package pipelines.data

import cats.syntax.functor._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import pipelines.core.{Rate, StreamStrategy}

sealed trait ModifyObservableRequest
object ModifyObservableRequest {
  case class AddStatistics(verbose: Boolean)                    extends ModifyObservableRequest
  case class Filter(expression: String)                         extends ModifyObservableRequest
  case class Persist(path: String)                              extends ModifyObservableRequest
  case class RateLimit(newRate: Rate, strategy: StreamStrategy) extends ModifyObservableRequest
  case class Take(limit: Int)                                   extends ModifyObservableRequest
  case class Generic(enrichmentKey: String, config: Json)       extends ModifyObservableRequest

  implicit val encodeEvent: Encoder[ModifyObservableRequest] = Encoder.instance {
    case enrichment @ AddStatistics(_) => enrichment.asJson
    case enrichment @ Persist(_)       => enrichment.asJson
    case enrichment @ RateLimit(_, _)  => enrichment.asJson
    case enrichment @ Filter(_)        => enrichment.asJson
    case enrichment @ Take(_)          => enrichment.asJson
    case enrichment @ Generic(_, _)    => enrichment.asJson

  }

  implicit val decodeEvent: Decoder[ModifyObservableRequest] =
    List[Decoder[ModifyObservableRequest]](
      Decoder[AddStatistics].widen,
      Decoder[Persist].widen,
      Decoder[Generic].widen,
      Decoder[RateLimit].widen,
      Decoder[Take].widen,
      Decoder[Filter].widen
    ).reduceLeft(_ or _)
}
