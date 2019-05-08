package pipelines.data

import io.circe.Json
import monix.reactive.Observable
import pipelines.core.{Rate, StreamStrategy}

import scala.concurrent.duration._

class DataRegistryTest extends BaseCoreTest {

  // we put this on the top-level as compiling filters is expensive, so ideally we actually do want to re-use them

  "backlog" ignore {
    // this one is just a zipped index mapping
    // this one creates a string index from the type A
    "be able to map a source type" in {
      ???
    }
    "be able to index a source to produce a new source" in {
      ???
    }
    "be able to transform json sources by adding/adjusting fields using donovan" in {
      ???
    }
    "be able to add statistics on a source" in {
      ???
    }
  }

  "DataRegistry" should {
    "be able to persist a source via the filesystem to produce a new source" in withScheduler { implicit sched =>
      Given("Some original json DataSource")
      val pushSource = DataSource.push[Int]
      val registry   = DataRegistry(sched)
      val sink       = DataSink.collect[Int]()
      registry.sources.register("source", pushSource)
      registry.sinks.register("sink", sink)

      WithTempDir { persistLocation =>

        registry.enrichSource("source", "source.persistent", ModifyObservable.Persist(persistLocation)) shouldBe SourceCreatedResponse("source.persistent", "byte[]")

        // from an int to bytes, then back to an Int
//        registry.as[Int]("source.persistent", "source.ints") shouldBe SourceCreatedResponse("source.ints", "int")

        registry.connect("source.ints", "sink") shouldBe ConnectResponse("source.ints", "sink")

        val expected = (0 to 10).map(pushSource.push).size

        eventually {
          import eie.io._
          persistLocation.children.size shouldBe expected
        }

      }
    }
    "be able to rate limit a source" in withScheduler { implicit sched =>
      // 100 messages/second
      val ints     = DataSource(Observable.interval(10.millis))
      val registry = DataRegistry(sched)
      registry.sources.register("ints", ints) shouldBe true

      val sink = DataSink.collect[Long]()
      registry.sinks.register("sink", sink) shouldBe true

      When("a rate limit is applied and connected")
      registry.rateLimitSources("ints", "ints.slow", Rate(1, 100.millis), StreamStrategy.Latest)
      registry.connect("ints.slow", "sink") shouldBe ConnectResponse("ints.slow", "sink")

      Then("The sink should only see a limited set")
      eventually {
        sink.toList().size should be >= 10
      }
      val received: List[Long] = sink.toList()

      val skipped = received.sliding(2, 1).exists {
        case List(a, b) => b > a + 1
        case _          => false
      }

      withClue(s"Rate limit doesn't seem to have been applied to ${received}") {
        skipped shouldBe true
      }
    }

    "be able to connect a registered source w/ a sink" in withScheduler { implicit sched =>
      val registry = DataRegistry(sched)
      registry.sources.register("foo", DataSource(Observable(1, 2, 3))) shouldBe true
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
      registry.sources.register("foo", DataSource(Observable(1, 2, 3))) shouldBe true
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

  override def testTimeout = 5.seconds
}
