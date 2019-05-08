package pipelines.data

import monix.reactive.Observable

class DataSourceTest extends BaseCoreTest {

  import MapTypeTest._

  "MapType.applyTo" should {
    "be able to apply a change which will converts a 'Thing' to bytes to persist, and then back to a 'Thing' to collect" in withScheduler { implicit sched =>
      val expectedThings: Seq[Thing] = (0 to 10).map(Thing.apply)

      WithScheduler { implicit sched =>
        WithTempDir { dir =>
          val persist = ModifyObservable.Persist(dir)

          val getThings: DataSink.Collect[Thing] = DataSink.Collect[Thing]()
          //
          // try to consumer an Observable[Array[Byte]] with Observer[Thing]
          //
          import MapTypeTest._

          // this is cool -- we start out w/ a dataSource of 'Thing'
          val original: DataSource[Thing] = DataSource(Observable.fromIterable(expectedThings))

          // and then end up w/ a data source of Array[Byte] due to the need to enrich it
          /*
          val source: DataSource[_] = original.enrich(persist).right.get

          // which then gets converted back to 'Thing' to be collected
          val Right(_) = source.connect(getThings)
          eventually {
            getThings.toList() shouldBe expectedThings
          }
           */

        // ... which of course is stupid :-(
        //
        // we need to teach it to keep the type in the tuple, so if we go from 'T' to 'Array[Byte]' we do so by (T, Array[Byte])
        // so that we can reuse either instead of redoing work
        }
      }
    }
  }
}
