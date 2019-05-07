package pipelines.data

import io.circe.Json
import monix.execution.Scheduler
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Span}
import org.scalatest.{GivenWhenThen, Matchers, WordSpec}

import scala.concurrent.duration._

abstract class BaseEvalTest extends WordSpec with Matchers with Eventually with GivenWhenThen {

  def withScheduler[A](f: Scheduler => A): A = WithScheduler(f)

  def testTimeout: FiniteDuration = 2.seconds

  def asJson(id: Int): Json = Json.obj("id" -> Json.fromInt(id), "name" -> Json.fromString(s"name-$id"))

  def idForJson(value: Json): Int = {
    value.asObject.map(_.toMap("id").asNumber.get.toInt.get).get
  }

  override implicit def patienceConfig = PatienceConfig(timeout = Span(testTimeout.toMillis, Millis), interval = Span(500, Millis))

}
