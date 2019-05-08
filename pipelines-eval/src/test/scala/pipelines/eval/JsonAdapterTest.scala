package pipelines.eval

import io.circe.Json
import monix.reactive.Observable
import pipelines.core.JsonRecord
import pipelines.data._

class JsonAdapterTest extends BaseEvalTest {
  implicit val filterAdapter = EvalFilterAdapter()

  "be able to update a filtered source" in withScheduler { implicit sched =>
    Given("Some original json DataSource")
    val jsonSource = DataSource.push[Json]

    And("A data registry with a filtered source")
    val registry = DataRegistry(sched)
    val sink     = DataSink.collect[Json]()
    registry.sinks.register("sink", sink)

    registry.sources.register("json", jsonSource) shouldBe true
    registry.sources.register("bytes", DataSource.push[Array[Byte]]) shouldBe true
    registry.filterSources("json", "json.filtered", """ value.id.asInt % 31 == 0 || value.name == "name-2" """) shouldBe SourceCreatedResponse("json.filtered", JsonRecord)
    registry.connect("json.filtered", "sink") shouldBe ConnectResponse("json.filtered", "sink")

    When("We push some data through")
    jsonSource.push(asJson(1))
    jsonSource.push(asJson(2))
    jsonSource.push(asJson(31))
    jsonSource.push(asJson(32))

    Then("The sink should receive the filtered values")
    eventually {
      sink.toList().size shouldBe 2
    }
    sink.toList().map(idForJson) shouldBe List(2, 31)

    When("We try and update a non-filter source")
    val SourceErrorResponse("bytes", errorMsg) = registry.updateFilterSource("bytes", "doesn't matter")

    Then("it should return an error")
    errorMsg shouldBe "Source 'bytes' for types ByteArray is not a filtered source, it is: PushSource"

    When("We update the filtered source w/ a new filter")
    val SourceUpdatedResponse("json.filtered", okMsg) = registry.updateFilterSource("json.filtered", "value.id > 100")

    Then("it should update the expression")
    okMsg shouldBe "Filter expression updated to: value.id > 100"

    When("the source then pushes data which matches both the original and new filter")
    sink.clear()
    jsonSource.push(asJson(62))
    jsonSource.push(asJson(93))
    jsonSource.push(asJson(100))
    jsonSource.push(asJson(101))
    jsonSource.push(asJson(102))

    Then("We should only see values which match the filter")
    eventually {
      sink.toList().size shouldBe 2
    }
    sink.toList().map(idForJson) shouldBe List(101, 102)
  }
  "be able to filter a source" in withScheduler { implicit sched =>
    Given("Some original json DataSource")
    val jsonSource = Observable.fromIterable(0 to 100).map { id =>
      asJson(id)
    }

    And("A data registry")
    val registry = DataRegistry(sched)
    registry.sources.register("json", DataSource(jsonSource, JsonRecord)) shouldBe true
    registry.filterSources("json", "json.filtered", """ value.id.asInt % 31 == 0 || value.name == "name-2" """) shouldBe SourceCreatedResponse("json.filtered", JsonRecord)

    When("We collect on a filtered source")
    val sink = DataSink.collect[Json]()
    registry.sinks.register("sink", sink)
    registry.connect("json.filtered", "sink")

    Then("We should only see values which match the filter")
    eventually {
      sink.toList().size shouldBe 5
    }
    sink.toList().map(idForJson) shouldBe List(0, 2, 31, 62, 93)
  }
}
