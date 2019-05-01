package pipelines.data

import io.circe.Json
import monix.reactive.Observable
import pipelines.expressions.JsonExpressions

class DataRegistryTest extends BaseEvalTest {

  "backlog" ignore {
    "be able to map a source type" in {
      ???
    }
    "be able to add statistics on a source" in {
      ???
    }
    "be able to add statistics on a sink" in {
      ???
    }
    "be able to rate limit a source" in {
      ???
    }
    "be able to persist a source via the filesystem to produce a new source" in {
      ???
    }
    "be able to persist a source via the filesystem" in {
      ???
    }
  }
  "DataRegistry" should {
    "be able to filter a source" in withScheduler { implicit sched =>
      val jsonSource = Observable.fromIterable(0 to 100).map { id =>
        Json.obj("id" -> Json.fromInt(id), "name" -> Json.fromString(s"name-$id"))
      }
      val registry              = DataRegistry(sched)
      implicit val createFilter = FilterAdapter()
      registry.sources.register("json", DataSource(jsonSource, JsonRecord)) shouldBe true
      registry.filterSources("json", "json.filtered", """ value.id.asInt % 31 == 0 || value.name == "name-2" """) shouldBe SourceCreatedResponse("json.filtered", JsonRecord)

      val sink = DataSink.collect[Json]()
      registry.sinks.register("sink", sink)

      registry.connect("json.filtered", "sink")

      eventually {
        sink.toList().size shouldBe 5
      }
      val values: List[Json] = sink.toList()
      val ids                = values.flatMap(_.asObject).map(_.toMap("id").asNumber.get.toInt.get)
      ids shouldBe List(0, 2, 31, 62, 93)
    }

    "be able to updated a filtered source" ignore {}
  }

  "passing" ignore {

    "be able to connect a registered source w/ a sink" in withScheduler { implicit sched =>
      val registry = DataRegistry(sched)
      registry.sources.register("foo", DataSource(Observable(1, 2, 3), AnyType)) shouldBe true
      val sink = DataSink.collect[Int]()
      registry.sinks.register("bar", sink) shouldBe true

      registry.connect("unknown", "bar") shouldBe SourceNotFoundResponse("unknown")
      registry.connect("foo", "unknown") shouldBe SinkNotFoundResponse("unknown")
      registry.connect("foo", "bar") shouldBe ConnectResponse("foo", "bar")

      eventually {
        sink.toList() shouldBe List(1, 2, 3)
      }
    }
    "be able to connect a registered source w/ a sink multiple times" in withScheduler { implicit sched =>
      val registry = DataRegistry(sched)
      registry.sources.register("foo", DataSource(Observable(1, 2, 3), AnyType)) shouldBe true
      val sink = DataSink.collect[Int]()
      registry.sinks.register("bar", sink) shouldBe true

      registry.connect("unknown", "bar") shouldBe SourceNotFoundResponse("unknown")
      registry.connect("foo", "unknown") shouldBe SinkNotFoundResponse("unknown")
      registry.connect("foo", "bar") shouldBe ConnectResponse("foo", "bar")

      eventually {
        sink.toList() shouldBe List(1, 2, 3)
      }
      registry.connect("foo", "bar") shouldBe ConnectResponse("foo", "bar")

      eventually {
        sink.toList() shouldBe List(1, 2, 3, 1, 2, 3)
      }
    }
  }
}
