package esa.mongo
import com.mongodb.ConnectionString
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.{MongoClient, MongoClientSettings, MongoCredential}

object MongoConnect {

  def main(args: Array[String]): Unit = {
    clientForUri("mongodb://localhost:9010")
  }

  def clientForUri(uri: String): MongoClient = {

    val creds = MongoCredential.createCredential("serviceUser", "esa", "changeTh1sDefaultPasswrd".toCharArray)
    MongoClientSettings
      .builder()
      .applyConnectionString(new ConnectionString(uri))
      .codecRegistry(DEFAULT_CODEC_REGISTRY)
      .credential(creds)
      .build()

    MongoClient(uri)
    //val client2: MongoClient = MongoClient()
  }

  def collection(client: MongoClient, dbName: String = "mydb", coll: String = "mycoll") = {
    val db = client.getDatabase(dbName)
    db.getCollection(coll)
  }
}
