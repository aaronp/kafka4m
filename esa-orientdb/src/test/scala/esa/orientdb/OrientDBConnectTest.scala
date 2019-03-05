package esa.orientdb

import esa.core.users.User
import gremlin.scala.{GremlinScala, ScalaGraph, Vertex}
import org.apache.tinkerpop.gremlin.structure.Edge
import org.scalatest.{Matchers, WordSpec}

class OrientDBConnectTest extends WordSpec with Matchers with LowPriorityOrientDbImplicits {

  "OrientDBConnect" should {
    "work" in {
//      val sgraph: ScalaGraph = OrientDBConnect.test("test", "foo", "bar")
      val sgraph: ScalaGraph = OrientDBConnect.test("test", "root", "rootpwd")
//      val sgraph: ScalaGraph = OrientDBConnect.inMemory()

      val user1  = User.create("test1", "em@il.com")
      val vertx1 = sgraph.createVertex(user1)

      val user2  = User.create("test2", "em@il.com")
      val vertx2 = sgraph.createVertex(user2)

      val created: Edge = vertx1.addEdge("created", vertx2)
      println(vertx1)
      println(vertx2)
      println(created)

      val v: GremlinScala[Vertex] = sgraph.V(vertx1.id())
      v.toList() shouldBe List(vertx1)
    }
  }
}
