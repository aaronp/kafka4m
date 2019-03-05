package esa.orientdb

import gremlin.scala.{ScalaGraph, Vertex}
import io.circe.Encoder
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

import scala.reflect.ClassTag

class RichScalaGraph(val sgraph: ScalaGraph) {

  def listVertices[T: ClassTag](): GraphTraversal[_, Vertex] = listVerticesForLabel(asLabel[T])

  def listVerticesForLabel[T: ClassTag](label: String): GraphTraversal[_, Vertex] = {
    sgraph.V().hasLabel(label).traversal
  }

  def asLabel[T: ClassTag](): String = implicitly[ClassTag[T]].runtimeClass.getName.filter(_.isLetterOrDigit)

  def createVertex[T: Encoder: ClassTag](value: T): Vertex = createVertex(asLabel[T], value)

  def createVertex[T: Encoder](label: String, value: T): Vertex = sgraph.addVertex(label, asProperties(value): _*)
}
