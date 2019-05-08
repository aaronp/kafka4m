package pipelines.rest.routes

import java.nio.file.Path
import java.util.UUID

import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import monix.execution.Scheduler
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{GivenWhenThen, Matchers, WordSpec}
import pipelines.data.WithScheduler

abstract class BaseRoutesTest extends WordSpec with Matchers with ScalatestRouteTest with FailFastCirceSupport with ScalaFutures with GivenWhenThen {

  def withScheduler[A](f: Scheduler => A): A = WithScheduler(f)

  def withTempDir[A](f: Path => A): A = {
    import eie.io._
    val dir = s"target/${getClass.getSimpleName}/${UUID.randomUUID}".asPath.mkDirs()
    try {
      f(dir)
    } finally {
      dir.delete()
    }
  }
}
