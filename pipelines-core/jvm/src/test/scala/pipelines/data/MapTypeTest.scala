package pipelines.data

import io.circe.{Decoder, ObjectEncoder}
import monix.reactive.Observable
import org.scalatest.concurrent.Eventually
import org.scalatest.{Matchers, WordSpec}
import pipelines.data.ModifyObservable.Take

class MapTypeTest extends WordSpec with Matchers with Eventually {

  import MapTypeTest._

  "MapType.connect" should {
    "be able to apply sinks to sources of different types" in {
      val expectedThings: Seq[Thing] = (0 to 10).map(Thing.apply)
      val thingBytes: Observable[Array[Byte]] = {
        import io.circe.syntax._
        Observable.fromIterable(expectedThings.map(_.asJson).map(_.noSpaces.getBytes("UTF-8")))
      }

      val getThings = DataSink.Collect[Thing]()
      WithScheduler { implicit sched =>
        //
        // try to consumer an Observable[Array[Byte]] with Observer[Thing]
        //
        /*
        val Right(_) = DataSource(thingBytes).enrich(Take(4)).connect(getThings)
        eventually {
          getThings.toList() shouldBe expectedThings.take(4)
        }
         */
      }
    }
  }

}

object MapTypeTest {

  case class Thing(x: Int)
  object Thing {
    implicit val encoder: ObjectEncoder[Thing] = io.circe.generic.semiauto.deriveEncoder[Thing]
    implicit val decoder: Decoder[Thing]       = io.circe.generic.semiauto.deriveDecoder[Thing]
  }

}
