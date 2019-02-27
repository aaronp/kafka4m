package esa.mongo
import io.circe.Encoder
import monix.reactive.Observable
import org.mongodb.scala.{Completed, Document, MongoCollection}

class RichCollection(val collection : MongoCollection[Document]) extends LowPriorityMongoImplicits {

  def insertOne[T : Encoder](value : T): Observable[Completed] = {
    collection.insertOne(BsonUtil.asDocument(value)).monix
  }
}
