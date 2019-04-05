package esa.orientdb

import com.typesafe.config.Config
import gremlin.scala._
import org.apache.tinkerpop.gremlin.orientdb.{OrientGraph, OrientGraphFactory}

object OrientDBConnect {

  /**
    * @DELETEME
    * @TODO
    * @FIXME
    * @param db
    * @param user
    * @param pw
    * @return
    */
  def test(db: String = "test", user: String = "root", pw: String = "rootpwd"): ScalaGraph = {
    //    apply(s"remote:localhost/$db", user, pw)
    apply(s"remote:localhost:2424/$db", user, pw)
  }

  def apply(config: Config): ScalaGraph = {
    apply(
      config.getString("url"),
      config.getString("user"),
      config.getString("password")
    )
  }

  def inMemory(db: String = "test"): ScalaGraph = {
    val conf = new org.apache.commons.configuration.BaseConfiguration()
    conf.setProperty(OrientGraph.CONFIG_URL, "memory:" + db)
    val factory = new OrientGraphFactory(conf)
    factory.getTx(true, true).asScala()
  }

  def apply(url: String, user: String, pw: String): ScalaGraph = {
    val factory             = new OrientGraphFactory(url, user, pw)
    val jgraph: OrientGraph = factory.getTx(true, true)
    //    val jgraph: OrientGraph = factory.getNoTx(true, true)
    //OrientGraph.open(url, user, pw)

    jgraph.asScala
  }
}
