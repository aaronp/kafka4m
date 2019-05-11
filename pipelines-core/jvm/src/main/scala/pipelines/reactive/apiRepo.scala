package pipelines.reactive

import cats.syntax.functor._
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json, ObjectEncoder}

/**
  * A request for operating on a repository
  */
sealed trait RepoRequest

object RepoRequest {
  implicit val encoder = Encoder.instance[RepoRequest] {
    case request @ ListRepoSourcesRequest(_)        => request.asJson
    case request @ CreateRepoSourceRequest(_, _, _) => request.asJson
    case request @ ListTransformationRequest(_)     => request.asJson
    case request @ ListSinkRequest()                => request.asJson
  }

  implicit val decoder: Decoder[RepoRequest] = {
    List[Decoder[RepoRequest]](
      Decoder[ListRepoSourcesRequest].widen,
      Decoder[CreateRepoSourceRequest].widen,
      Decoder[ListTransformationRequest].widen,
      Decoder[ListSinkRequest].widen
    ).reduceLeft(_ or _)
  }
}

/**
  * The response to a [[RepoRequest]]
  */
sealed trait RepoResponse
object RepoResponse {
  implicit val encoder = Encoder.instance[RepoResponse] {
    case request @ ListRepoSourcesResponse(_) => request.asJson
  }

  implicit val decoder: Decoder[RepoResponse] = {
    List[Decoder[RepoResponse]](
      Decoder[ListRepoSourcesResponse].widen
    ).reduceLeft(_ or _)
  }
}


final case class DataTransform(name: String, configuration: Option[Json] = None)
object DataTransform {
  implicit val encoder: ObjectEncoder[DataTransform] = deriveEncoder[DataTransform]
  implicit val decoder: Decoder[DataTransform]       = deriveDecoder[DataTransform]
}

final case class ListedDataSource(name: String, contentType: Option[ContentType])

object ListedDataSource {
  implicit val encoder: ObjectEncoder[ListedDataSource] = deriveEncoder[ListedDataSource]
  implicit val decoder: Decoder[ListedDataSource]       = deriveDecoder[ListedDataSource]
}

final case class ListedSink(name: String) //, contentType: Option[ContentType])
object ListedSink {
  implicit val encoder: ObjectEncoder[ListedSink] = deriveEncoder[ListedSink]
  implicit val decoder: Decoder[ListedSink]       = deriveDecoder[ListedSink]
}

final case class ListedTransformation(name: String, resultType: Option[ContentType])
object ListedTransformation {
  implicit val encoder: ObjectEncoder[ListedTransformation] = deriveEncoder[ListedTransformation]
  implicit val decoder: Decoder[ListedTransformation]       = deriveDecoder[ListedTransformation]
}

/**
  *  LIST DATA SOURCE
  */
case class ListRepoSourcesRequest(ofType: Option[ContentType]) extends RepoRequest
object ListRepoSourcesRequest {
  implicit val encoder: ObjectEncoder[ListRepoSourcesRequest] = deriveEncoder[ListRepoSourcesRequest]
  implicit val decoder: Decoder[ListRepoSourcesRequest]       = deriveDecoder[ListRepoSourcesRequest]
}

case class ListRepoSourcesResponse(results: Seq[ListedDataSource]) extends RepoResponse
object ListRepoSourcesResponse {
  implicit val encoder: ObjectEncoder[ListRepoSourcesResponse] = deriveEncoder[ListRepoSourcesResponse]
  implicit val decoder: Decoder[ListRepoSourcesResponse]       = deriveDecoder[ListRepoSourcesResponse]
}

/**
  *  LIST TRANSFORMATION
  */
case class ListTransformationRequest(inputContentType: Option[ContentType]) extends RepoRequest
object ListTransformationRequest {
  implicit val encoder: ObjectEncoder[ListTransformationRequest] = deriveEncoder[ListTransformationRequest]
  implicit val decoder: Decoder[ListTransformationRequest]       = deriveDecoder[ListTransformationRequest]
}

case class ListTransformationResponse(results: Seq[ListedTransformation]) extends RepoResponse
object ListTransformationResponse {
  implicit val encoder: ObjectEncoder[ListTransformationResponse] = deriveEncoder[ListTransformationResponse]
  implicit val decoder: Decoder[ListTransformationResponse]       = deriveDecoder[ListTransformationResponse]
}

/**
  *  LIST SINKS
  */
//case class ListSinkRequest(inputContentType: Option[ContentType]) extends DataRequest
case class ListSinkRequest() extends RepoRequest
object ListSinkRequest {
  implicit val encoder: ObjectEncoder[ListSinkRequest] = deriveEncoder[ListSinkRequest]
  implicit val decoder: Decoder[ListSinkRequest]       = deriveDecoder[ListSinkRequest]
}

case class ListSinkResponse(results: Seq[ListedSink]) extends RepoResponse
object ListSinkResponse {
  implicit val encoder: ObjectEncoder[ListSinkResponse] = deriveEncoder[ListSinkResponse]
  implicit val decoder: Decoder[ListSinkResponse]       = deriveDecoder[ListSinkResponse]
}

/**
  * CREATE DATA SOURCE from an existing source and transformation
  */
case class CreateRepoSourceRequest(baseDataSource: String, transformations: Seq[String], newDataSourceName: Option[String] = None) extends RepoRequest
object CreateRepoSourceRequest {
  implicit val encoder: ObjectEncoder[CreateRepoSourceRequest] = deriveEncoder[CreateRepoSourceRequest]
  implicit val decoder: Decoder[CreateRepoSourceRequest]       = deriveDecoder[CreateRepoSourceRequest]
}

case class CreateRepoSourceResponse(dataSource: String) extends RepoResponse
object CreateRepoSourceResponse {
  implicit val encoder: ObjectEncoder[CreateRepoSourceResponse] = deriveEncoder[CreateRepoSourceResponse]
  implicit val decoder: Decoder[CreateRepoSourceResponse]       = deriveDecoder[CreateRepoSourceResponse]
}
