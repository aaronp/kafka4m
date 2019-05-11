package pipelines.reactive

import cats.syntax.functor._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, ObjectEncoder}

/**
  * Represents available content types
  */
sealed trait ContentType

object ContentType {

  implicit val encoder = Encoder.instance[ContentType] {
    case request @ SimpleContentType(_) => request.asJson
    case request @ ClassType(_, _)      => request.asJson
  }

  implicit val decoder: Decoder[ContentType] = {
    List[Decoder[ContentType]](
      Decoder[ClassType].widen,
      Decoder[SimpleContentType].widen
    ).reduceLeft(_ or _)
  }

  import scala.reflect.runtime.universe._

  def of[A](implicit typeTag: TypeTag[A]): ContentType = {
    typeTag.tpe match {
      case tr @ TypeRef(_, _, _) => forTypeRef(tr)
      case tr @ ExistentialType(_, typ) =>
        typ match {
          case ref: TypeRef => forTypeRef(ref)
          case _            => ClassType(tr.toString, Nil)
        }
    }
  }

  private def forTypeRef(typeRef: TypeRef): ClassType = {
    val TypeRef(x, sy, args) = typeRef
    if (args.isEmpty) {
      if (x.termSymbol.isTerm) {
        ClassType(sy.name.toString, Nil)
      } else {
        ClassType(sy.name.toString, Nil)
      }
    } else {
      val params: List[ClassType] = args.map {
        case other @ TypeRef(_, _, _) => forTypeRef(other)
        case tr @ ExistentialType(_, typ) =>
          typ match {
            case tr @ TypeRef(_, _, _) => forTypeRef(tr)
            case _                     => ClassType(tr.toString, Nil)
          }
        case arg => ClassType(arg.toString, Nil)
      }
      ClassType(sy.name.toString, params)
    }
  }

  def apply(name: String): ContentType = {
    SimpleContentType(name.toLowerCase.trim)
  }
}

case class SimpleContentType(name: String) extends ContentType
object SimpleContentType {
  implicit val encoder: ObjectEncoder[SimpleContentType] = io.circe.generic.semiauto.deriveEncoder[SimpleContentType]
  implicit val decoder: Decoder[SimpleContentType]       = io.circe.generic.semiauto.deriveDecoder[SimpleContentType]
}

case class ClassType(className: String, params: Seq[ClassType] = Nil) extends ContentType {
  override def toString = {
    if (params.isEmpty) {
      className
    } else {
      params.mkString(s"${className}[", ",", "]")
    }
  }
}
object ClassType {
  implicit val encoder: ObjectEncoder[ClassType] = io.circe.generic.semiauto.deriveEncoder[ClassType]
  implicit val decoder: Decoder[ClassType]       = io.circe.generic.semiauto.deriveDecoder[ClassType]

}
