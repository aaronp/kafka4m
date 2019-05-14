package pipelines.data

import monix.execution.Scheduler
import monix.execution.schedulers.SchedulerService
import org.scalatest.BeforeAndAfterAll

trait TestWideScheduler extends BaseCoreTest with BeforeAndAfterAll {

  implicit lazy val scheduler: SchedulerService = Scheduler.io()

  override def afterAll(): Unit = {
    super.afterAll()
    scheduler.shutdown()
  }

}
