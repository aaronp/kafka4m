package pipelines.data

import monix.reactive.{Observer, Pipe}
import pipelines.core.AnyType

import scala.collection.mutable.ListBuffer

class DataSourceFilteredTest extends BaseEvalTest {

  "DataSourceFiltered" should {
    "apply filters to a stream as they are updated" in {
      WithScheduler { implicit s =>
        Given("A filtered data source which we can send values through")
        val (pushNext, ints)          = Pipe.publishToOne[Int].unicast
        val (applyFilter, dataSource) = DataSourceFiltered.from(DataSource(ints, AnyType("string")))

        val list = ListBuffer[Int]()
        dataSource.data.foreach { next =>
          list += next
        }

        When("we send some first values")
        pushNext.onNext(1)
        pushNext.onNext(2)
        pushNext.onNext(3)

        Then("we should be able to observe those values")
        eventually {
          list should contain only (1, 2, 3)
        }

        When("We update the filter and push some more values")
        applyFilter := (_ % 3 == 0)
        Observer.feed(pushNext, (4 to 13))

        Then("We should only see values which match the new filter")
        eventually {
          list should contain only (1, 2, 3, 6, 9, 12)
        }

        list.toList shouldBe List(1, 2, 3, 6, 9, 12)
      }
    }
  }
}
