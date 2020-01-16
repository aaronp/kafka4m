package kafka4m

import java.nio.file.Path
import java.util.concurrent.atomic.AtomicLong

import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._

abstract class BaseKafka4mSpec extends AnyWordSpec with Matchers with ScalaFutures with Eventually {

  def testTimeout: FiniteDuration = 5.seconds

  implicit override def patienceConfig =
    PatienceConfig(timeout = scaled(Span(testTimeout.toSeconds, Seconds)), interval = scaled(Span(150, Millis)))

  def withTmpDir(f: Path => Unit): Unit = BaseKafka4mSpec.withTmpDir(f)
}
object BaseKafka4mSpec {

  private val counter = new AtomicLong(System.currentTimeMillis())
  def withTmpDir(f: Path => Unit): Unit = {
    import eie.io._
    val dir = s"target/${getClass.getSimpleName}-${counter.incrementAndGet}".asPath.mkDirs()
    try {
      f(dir)
    } finally {
      dir.delete()
    }
  }
}
