package pipelines.data

import monix.reactive.Observable

import scala.collection.mutable.ListBuffer

class SourceRegistryTest extends BaseEvalTest {

  "Source Registry" should {
    "notify listeners when a registry is created and removed" in withScheduler { implicit sched =>
      Given("A registry")
      val registry = SourceRegistry(sched)
      val events   = ListBuffer[SourceRegistryEvent]()
      registry.events.foreach(events += _)

      When("A data source is registered")
      registry.register("foo", DataSource(Observable(1), AnyType))

      Then("A notification should be published")
      eventually {
        events.size shouldBe 1
      }
      val DataSourceRegistered("foo", ds) = events.head

      And("the registry should contain the data source")
      registry.keys shouldBe Set("foo")

      When("The data source is removed")
      registry.remove("doesn't exist") shouldBe false
      registry.remove("foo") shouldBe true

      Then("a remove notification should be published")
      eventually {
        events.size shouldBe 2
      }
      events.last shouldBe DataSourceRemoved("foo", ds)
      registry.keys should be(empty)
    }
  }

}
