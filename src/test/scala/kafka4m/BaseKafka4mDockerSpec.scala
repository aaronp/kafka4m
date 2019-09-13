package kafka4m

import com.typesafe.scalalogging.StrictLogging
import dockerenv.BaseKafkaSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen}

import scala.concurrent.duration._

abstract class BaseKafka4mDockerSpec extends BaseKafkaSpec with ScalaFutures with BeforeAndAfterAll with GivenWhenThen with StrictLogging {

  override def testTimeout: FiniteDuration = 15.seconds

}
