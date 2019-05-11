package pipelines.reactive

import io.circe.Json
import monix.reactive.Observable
import pipelines.data.BaseCoreTest

import scala.util.Try

class RepositoryTest extends BaseCoreTest {
  import RepositoryTest._

  "Repository.createChain" should {
    "be able to consume data from each phase in a pipeline" in {
      val Right(resp) = repo.createChain(CreateChainRequest("ints", Seq(DataTransform("evens", None))))
      resp.keys shouldBe Seq(0, 1)
    }
    "error when given invalid sources" in {
      val Left(err) = repo.createChain(CreateChainRequest("missing", Seq(DataTransform("evens", None))))
      err shouldBe "Couldn't find source for 'missing'"
    }
    "error when given invalid transforms" in {
      val Left(err) = repo.createChain(CreateChainRequest("ints", Seq(DataTransform("bad", None), DataTransform("worse", None))))
      err shouldBe "Found 2 error(s) creating a pipeline for transformation(s) 'bad', 'worse' : Couldn't find any transform for 'bad'; Couldn't find any transform for 'worse'"
    }
  }
  "Repository.listSources" should {
    "return all results when no content-type is specified" in {
      repo.listSources(ListRepoSourcesRequest(None)).results should contain only (
        ListedDataSource("ints", Option(ContentType.of[Int])),
        ListedDataSource("strings", Option(ContentType.of[String]))
      )
    }
    "return filtered results when a content-type is specified" in {
      Repository("ints" -> ints, "strings" -> strings).listSources(ListRepoSourcesRequest(Some(ContentType.of[String]))).results should contain only (
        ListedDataSource("strings",
                         Option(ContentType.of[String]))
      )
    }
  }
  "Repository.listSinks" should {
    "return a single result by default" in {
      Repository().listSinks(ListSinkRequest()).results should contain only (ListedSink("sink"))
    }
  }
}
object RepositoryTest {
  def ints    = Data(Observable.fromIterable(0 to 100))
  def strings = Data(Observable.fromIterable(0 to 100).map(_.toString))
  def even = Transform[Int, Int] { obs =>
    obs.filter(_ % 2 == 0)
  }
  def modFilter: ConfigurableTransform[ConfigurableTransform.FilterExpression] = ConfigurableTransform.jsonFilter { expr =>
    Try(expr.expression.toInt).toOption.map { x => (json: Json) =>
      json.asNumber.flatMap(_.toInt).exists(_ % x == 0)
    }
  }
  def repo =
    Repository("ints" -> ints, "strings" -> strings) //
      .withTransform("evens", even)                                 //
      .withConfigurableTransform("modFilter", modFilter)            //
      .withTransform("stringToJson", Transform.jsonEncoder[String]) //

}
