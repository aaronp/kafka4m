package kafka4m

import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

abstract class BaseKafka4mSpec extends WordSpec with Matchers with ScalaFutures with Eventually {

  def testTimeout: FiniteDuration = 5.seconds

  implicit override def patienceConfig =
    PatienceConfig(timeout = scaled(Span(testTimeout.toSeconds, Seconds)), interval = scaled(Span(150, Millis)))

  def withTmpDir(f: Path => Unit): Unit = BaseKafka4mSpec.withTmpDir(f)
}
object BaseKafka4mSpec {

  private val counter = new AtomicInteger(0)
  def withTmpDir(f: Path => Unit): Unit = {
    import eie.io._
    val dir = s"target/${getClass}-${counter.incrementAndGet}".asPath.mkDirs()
    try {
      f(dir)
    } finally {
      dir.delete()
    }
  }
}
