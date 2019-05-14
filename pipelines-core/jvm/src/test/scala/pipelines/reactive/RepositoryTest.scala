package pipelines.reactive

import pipelines.data.BaseCoreTest

class RepositoryTest extends BaseCoreTest with RepoTestData {

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