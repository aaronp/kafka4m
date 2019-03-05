package esa.orientdb

import gremlin.scala.ScalaGraph

trait LowPriorityOrientDbImplicits {
  implicit def asRichGraph(sgraph: ScalaGraph) = new RichScalaGraph(sgraph)

}
