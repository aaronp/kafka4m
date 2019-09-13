package kafka4m

import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

abstract class BaseKafka4mSpec extends WordSpec with Matchers with ScalaFutures with Eventually {

  def testTimeout: FiniteDuration = 5.seconds

  implicit override def patienceConfig =
    PatienceConfig(timeout = scaled(Span(testTimeout.toSeconds, Seconds)), interval = scaled(Span(150, Millis)))

}
