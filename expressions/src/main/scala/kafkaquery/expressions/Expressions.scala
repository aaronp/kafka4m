package kafkaquery

package expressions

import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox

/**
  * https://docs.scala-lang.org/overviews/reflection/environment-universes-mirrors.html
  * https://stackoverflow.com/questions/11055210/whats-the-easiest-way-to-use-reify-get-an-ast-of-an-expression-in-scala/25195837
  *
  */
object Expressions {

  object cache {

    private object Lock

    private var predicateByRule = Map[String, Predicate]()

    private def createUnsafe(rule: String): Predicate = {
      val p = Predicate(rule)
      predicateByRule = predicateByRule.updated(rule, p)
      p
    }

    def apply(rule: String): Predicate = {
      Lock.synchronized(predicateByRule.getOrElse(rule, createUnsafe(rule)))
    }
  }

  def Predicate(expr: String): Predicate = asPredicate(expr) match {
    case Right(predicate) => predicate
    case Left(err)        => sys.error(err)
  }

  def asPredicate(expr: String): Either[String, Predicate] = {
    val script =
      s"""import kafkaquery.expressions._
         |import Expressions._
         |
         |(input : Record) => {
         |  val value = asDynamic(input)
         |  $expr
         |}
       """.stripMargin

    try {
      val tree   = compiler.parse(script)
      val result = compiler.eval(tree)
      result match {
        case p: Predicate => Right(p)
        case other        => Left(s"Couldn't parse '$script' as a Predicate : $other")
      }
    } catch {
      case err => Left(s"Couldn't parse '$script' as a Predicate : $err")
    }
  }

  private lazy val compiler = currentMirror.mkToolBox()

}
