package pipelines.reactive

import io.circe.{Decoder, Json, ObjectEncoder}
import monix.execution.Scheduler
import monix.reactive.Observable
import pipelines.data.TestWideScheduler

import scala.util.Try

trait RepoTestData extends TestWideScheduler {
  def ints           = Data(Observable.fromIterable(0 to 100))
  def strings        = Data(Observable.fromIterable(0 to 100).map(_.toString))
  def isEven(x: Int) = x % 2 == 0
  def isOdd(x: Int)  = x % 2 == 1
  def even           = Transform[Int, Int](_.filter(isEven))
  def odd            = Transform[Int, Int](_.filter(isOdd))
  def modFilter: ConfigurableTransform[ConfigurableTransform.FilterExpression] = ConfigurableTransform.jsonFilter { expr =>
    Try(expr.expression.toInt).toOption.map { x => (json: Json) =>
      json.asNumber.flatMap(_.toInt).exists(_ % x == 0)
    }
  }

  val asData: Transform     = Transform.map(RepoTestData.TestData.apply)
  val asJson: Transform     = Transform.jsonEncoder[RepoTestData.TestData]
  val jsonToStr: Transform  = Transform.jsonToString
  val strToBytes: Transform = Transform.stringToUtf8

  def repo : Repository =
    Repository("ints" -> ints, "strings" -> strings) //
      .withTransform("strToBytes", strToBytes)                      //
      .withTransform("jsonToStr", jsonToStr)                        //
      .withTransform("TestData to json", asJson)                    //
      .withTransform("int to TestData", asData)                     //
      .withTransform("evens", even)                                 //
      .withTransform("odds", odd)                                   //
      .withConfigurableTransform("modFilter", modFilter)            //
      .withTransform("stringToJson", Transform.jsonEncoder[String]) //

}

object RepoTestData {

  case class TestData(value: Int)
  object TestData {
    implicit val encoder: ObjectEncoder[TestData] = io.circe.generic.semiauto.deriveEncoder[TestData]
    implicit val decoder: Decoder[TestData]       = io.circe.generic.semiauto.deriveDecoder[TestData]
  }
}
