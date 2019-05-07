package pipelines.core

import cats.syntax.functor._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

/**
  * An enrichment represents something we can do to a stream (filter it, rate limit it, etc).
  *
  * Once the stream has been enriched, some scenarios allow for updating an updated stream (e.g. update the rate limit)
  */
sealed trait Enrichment

object Enrichment {
  implicit val encodeEvent: Encoder[Enrichment] = Encoder.instance {
    case enrichment @ PersistData(_)   => enrichment.asJson
    case enrichment @ AddStatistics(_) => enrichment.asJson
    case enrichment @ MapType(_)       => enrichment.asJson
    case enrichment @ RateLimit(_, _)  => enrichment.asJson
    case enrichment @ Filter(_)        => enrichment.asJson

  }

  implicit val decodeEvent: Decoder[Enrichment] =
    List[Decoder[Enrichment]](
      Decoder[PersistData].widen,
      Decoder[AddStatistics].widen,
      Decoder[MapType].widen,
      Decoder[RateLimit].widen,
      Decoder[Filter].widen
    ).reduceLeft(_ or _)

  case class PersistData(path: String)                          extends Enrichment
  case class AddStatistics(verbose: Boolean)                    extends Enrichment
  case class MapType(newType: DataType)                         extends Enrichment
  case class RateLimit(newRate: Rate, strategy: StreamStrategy) extends Enrichment
  case class Filter(filterExpression: String)                   extends Enrichment
}
