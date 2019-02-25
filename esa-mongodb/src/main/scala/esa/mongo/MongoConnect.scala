package esa.mongo
import com.mongodb.ConnectionString
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.{MongoClient, MongoClientSettings}

object MongoConnect {

  def main(args: Array[String]): Unit = {
    clientForUri("mongodb://localhost:32768")
  }

  def clientForUri(uri: String): MongoClient = {

    MongoClientSettings
      .builder()
      .applyConnectionString(new ConnectionString(uri))
      .codecRegistry(DEFAULT_CODEC_REGISTRY)
      .build()

    MongoClient(uri)
    //val client2: MongoClient = MongoClient()
  }

  def collection(client: MongoClient, db: String = "mydb", coll: String = "mycoll") = {
    val db = client.getDatabase(db)
    db.getCollection(coll)
  }
}
