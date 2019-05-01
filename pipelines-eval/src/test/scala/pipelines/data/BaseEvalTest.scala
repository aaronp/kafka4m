package pipelines.data

import monix.execution.Scheduler
import org.scalatest.{GivenWhenThen, Matchers, WordSpec}
import org.scalatest.concurrent.Eventually

import concurrent.duration._

abstract class BaseEvalTest extends WordSpec with Matchers with Eventually with GivenWhenThen {

  def withScheduler[A](f: Scheduler => A): A = WithScheduler(f)

  def testTimeout                            = 2.seconds
}
