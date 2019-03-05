package esa.mongo
import io.circe
import io.circe.Decoder
import org.mongodb.scala.Document

class RichDocument(val document: Document) extends AnyVal {

  def as[A: Decoder]: Either[circe.Error, A] = {
    io.circe.parser.decode[A](document.toJson())
  }
}
