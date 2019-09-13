package kafka4m

import com.typesafe.scalalogging.StrictLogging
import dockerenv.BaseKafkaSpec
import kafka4m.util.KafkaThreads
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

abstract class BaseKafka4mDockerSpec extends BaseKafkaSpec with ScalaFutures with BeforeAndAfterAll with GivenWhenThen with StrictLogging {

  override def testTimeout: FiniteDuration = 5.seconds

  override def afterAll(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits._
    Future {
      Thread.sleep(10000)
      val threads = KafkaThreads()

      if (threads.nonEmpty) {
        logger.warn(s"FORCE CLOSING ${threads.size} THREADS: ${threads}")
        threads.par.foreach(t => Try(t.stop()))
      }
    }
  }
}
