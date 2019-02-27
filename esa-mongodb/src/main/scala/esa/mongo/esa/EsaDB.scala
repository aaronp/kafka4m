package esa.mongo.esa
import esa.mongo.{LowPriorityMongoImplicits, RichCollection}
import org.mongodb.scala.MongoDatabase

class EsaDB(val mongo: MongoDatabase) extends LowPriorityMongoImplicits {

  def users: RichCollection = mongo.getCollection("users")

}
