package pipelines.reactive

import io.circe.syntax._
import io.circe.{Decoder, Json}
import monix.execution.atomic.AtomicAny
import monix.reactive.Observable
import pipelines.core.Rate
import pipelines.data.select

import scala.util.Try

/**
  * Provides a means to perform an update on an in-place transform (e.g. update a rate, filter, etc)
  *
  * @tparam A
  */
trait ConfigurableTransform[A] {

  /**
    * return the updated transformation
    *
    * @param config
    * @return the updated transformation
    */
  def update(config: A): Transform

  def updateFromJson(config: Json) = {
    val tri: Try[A] = jsonDecoder.decodeJson(config).toTry
    tri.map { c =>
      update(c)
    }
  }

  def defaultTransform(): Try[Transform] = updateFromJson(configJson)

  def jsonDecoder: Decoder[A]
  def configJson: Json
}

object ConfigurableTransform {

  type JsonTransform = ConfigurableTransform[Json]

  case class FilterExpression(expression: String)
  object FilterExpression {
    implicit val encoder = io.circe.generic.semiauto.deriveEncoder[FilterExpression]
    implicit val decoder = io.circe.generic.semiauto.deriveDecoder[FilterExpression]
  }

  type JsonPredicate = Json => Boolean

  def jsonFilter(applyFilter: FilterExpression => Option[JsonPredicate], initial: JsonPredicate = _ => true): ConfigurableTransform[FilterExpression] = {
    new ConfigurableTransform[FilterExpression] {
      val filterVar = AtomicAny[(FilterExpression, Json => Boolean)](FilterExpression("true") -> initial)
      lazy val underlying: Transform = Transform.flatMap[Json, Json] { input =>
        val (_, predicate) = filterVar.get
        if (predicate(input)) {
          Observable(input)
        } else {
          Observable.empty
        }
      }
      override def update(config: FilterExpression): Transform = {
        applyFilter(config).foreach { predicate =>
          filterVar.set(config -> predicate)
        }
        underlying
      }
      override def jsonDecoder: Decoder[FilterExpression] = FilterExpression.decoder
      override def configJson: Json                       = filterVar.get._1.asJson
    }
  }

  import scala.reflect.runtime.universe.TypeTag

  def rateLimitAll[A: TypeTag](initial: Rate): ConfigurableTransform[Rate] = {
    new ConfigurableTransform[Rate] {
      val limitRef = AtomicAny[Rate](initial)
      lazy val underlying: Transform = Transform.apply[A, A] { data =>
        data.bufferTimed(limitRef.get.per).map(select(_, limitRef.get.messages)).switchMap(Observable.fromIterable)
      }
      override def update(config: Rate): Transform = {
        limitRef.set(config)
        underlying
      }
      override def jsonDecoder      = Rate.RateDecoder
      override def configJson: Json = limitRef.get.asJson
    }
  }

  def rateLimitLatest[A: TypeTag](initial: Rate): ConfigurableTransform[Rate] = {
    new ConfigurableTransform[Rate] {
      val limitRef = AtomicAny[Rate](initial)
      lazy val underlying: Transform = Transform.apply[A, A] { data =>
        data.bufferTimed(limitRef.get.per).whileBusyDropEvents.map(select(_, limitRef.get.messages)).switchMap(Observable.fromIterable)
      }
      override def update(config: Rate) = {
        limitRef.set(config)
        underlying
      }
      override def jsonDecoder = Rate.RateDecoder

      override def configJson: Json = limitRef.get.asJson
    }
  }
}
