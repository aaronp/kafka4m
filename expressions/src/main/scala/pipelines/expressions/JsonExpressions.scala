package pipelines.expressions

import io.circe._

import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal

/** Exposes a means of converting text syntax based on circe optics into a 'Json => Boolean' predicate
  */
object JsonExpressions {

  type Predicate = Json => Boolean

  def newCache: Cache[Predicate] = new Cache[Predicate]((asPredicate _), _ => true)

  def asPredicate(inputExpr: String): Try[Predicate] = {
    val expr = inputExpr.replaceAllLiterally(" == ", " ===== ")
    val script =
      s"""import pipelines.expressions.implicits._
         |import io.circe.optics.JsonPath._
         |import io.circe._
         |
         |(inputJson : Json) => {
         |  implicit val implicitInputJson = inputJson
         |  val value = root
         |  $expr
         |}
       """.stripMargin

    try {
      val tree   = compiler.parse(script)
      val result = compiler.eval(tree)
      result match {
        case p: Predicate => Success(p)
        case other        => Failure(new Exception(s"Couldn't parse '$script' as a json predicate : $other"))
      }
    } catch {
      case NonFatal(err) =>
        Failure(new Exception(s"Couldn't parse '$script' as a json predicate : $err", err))
    }
  }

  private def compiler = AvroExpressions.compiler

}
