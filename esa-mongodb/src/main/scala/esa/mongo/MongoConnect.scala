package esa.mongo
import com.mongodb.ConnectionString
import com.typesafe.config.Config
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.{MongoClient, MongoClientSettings, MongoCredential}

/**
* Functions to knock out a MongoClient
  */
object MongoConnect {

  def apply(config : Config): MongoClient = {
    apply(
      config.getString("user"),
      config.getString("password").toCharArray,
      config.getString("database"),
      config.getString("uri"),
    )
  }

  /**
    * create a connection given the user, password, database and uri.
    *
    * NOTE: THE PASSWORD ARRAY IS MUTATED/CLEARED! Long live side-effects! (j/k ... FP i <3 you! )
    *
    * @param user
    * @param pw
    * @param database
    * @param uri
    * @return a MongoClient
    */
  def apply(user : String, pw : Array[Char], database : String, uri: String): MongoClient = {
    val creds = MongoCredential.createCredential(user, database, pw)
    MongoClientSettings
      .builder()
      .applyConnectionString(new ConnectionString(uri))
      .codecRegistry(DEFAULT_CODEC_REGISTRY)
      .credential(creds)
      .build()
    MongoClient(uri)
  }

  def clear(pw : Array[Char]) = pw.indices.foreach(pw.update(_, '*'))

  def collection(client: MongoClient, dbName: String = "mydb", coll: String = "mycoll") = {
    val db = client.getDatabase(dbName)
    db.getCollection(coll)
  }
}
