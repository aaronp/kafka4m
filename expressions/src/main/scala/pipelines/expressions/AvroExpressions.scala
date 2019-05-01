package pipelines

package expressions

import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox
import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal

/**
  * https://docs.scala-lang.org/overviews/reflection/environment-universes-mirrors.html
  * https://stackoverflow.com/questions/11055210/whats-the-easiest-way-to-use-reify-get-an-ast-of-an-expression-in-scala/25195837
  *
  */
object AvroExpressions {
  type Predicate = Record => Boolean

  def newCache: Cache[Predicate] = new Cache[Predicate]((asPredicate _), _ => true)

  def Predicate(expr: String): Predicate = asPredicate(expr).get

  def asPredicate(expr: String): Try[Predicate] = {
    val script =
      s"""import pipelines.expressions._
         |import AvroExpressions._
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
        case p: Predicate => Success(p)
        case other        => Failure(new Exception(s"Couldn't parse '$script' as a AvroPredicate : $other"))
      }
    } catch {
      case NonFatal(err) => Failure(new Exception(s"Couldn't parse '$script' as a Predicate : $err", err))
    }
  }

  private[expressions] lazy val compiler = currentMirror.mkToolBox()

}
