package esa.mongo
import io.circe
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import org.mongodb.scala.bson.collection._

/**
  * TODO find or create a library which maps BsonValues to circe json types
  */
object BsonUtil {
  def asDocument[A: Encoder](value: A): immutable.Document = asDocument(value.asJson)
  def asDocument(json: Json): immutable.Document           = immutable.Document(json.noSpaces)
  def asMutableDocument(json: Json): mutable.Document      = mutable.Document(json.noSpaces)

  def parse[A: Decoder](doc: immutable.Document): Either[circe.Error, A] = {
    decode[A](doc.toJson())
  }
}
