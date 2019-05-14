package pipelines.reactive

import io.circe.Json
import monix.reactive.Observable

import scala.util.Try

trait RepoTestData {
  def ints           = Data(Observable.fromIterable(0 to 100))
  def strings        = Data(Observable.fromIterable(0 to 100).map(_.toString))
  def isEven(x: Int) = x % 2 == 0
  def isOdd(x: Int)  = x % 2 == 1
  def even           = Transform[Int, Int](_.filter(isEven))
  def odd = Transform[Int, Int](_.filter(isOdd))
  def modFilter: ConfigurableTransform[ConfigurableTransform.FilterExpression] = ConfigurableTransform.jsonFilter { expr =>
    Try(expr.expression.toInt).toOption.map { x => (json: Json) =>
      json.asNumber.flatMap(_.toInt).exists(_ % x == 0)
    }
  }
  def repo =
    Repository("ints" -> ints, "strings" -> strings) //
      .withTransform("evens", even)                                 //
      .withTransform("odds", odd)                                   //
      .withConfigurableTransform("modFilter", modFilter)            //
      .withTransform("stringToJson", Transform.jsonEncoder[String]) //

}
