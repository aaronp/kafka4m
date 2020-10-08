package kafka4m

import java.nio.file.Path

import com.typesafe.scalalogging.StrictLogging
import dockerenv.BaseDockerSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen}

import scala.concurrent.duration._

abstract class BaseKafka4mDockerSpec
    extends BaseDockerSpec(dockerenv.kafka().withLogger(dockerenv.stdOut))
    with ScalaFutures
    with BeforeAndAfterAll
    with GivenWhenThen
    with StrictLogging {

  // travis can be quite slow
  override def testTimeout: FiniteDuration = 15.seconds

  def withTmpDir(f: Path => Unit): Unit = BaseKafka4mSpec.withTmpDir(f)
}
