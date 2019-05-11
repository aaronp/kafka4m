package pipelines.data.shape

/**
  * A basic type representation we can match on
  */
sealed trait Shape {
  def name: String
}
final case class ParameterizedShape(override val name: String, of: Seq[Shape]) extends Shape {
  override def toString: String = {
    of.mkString(s"$name[", ",", "]")
  }
}
final case class SimpleShape(override val name: String) extends Shape {
  override def toString: String = name
}
final case class ParamShape(override val name: String) extends Shape {
  override def toString: String = name
}

object Shape {

  import scala.reflect.runtime.universe._

  def apply(name: String): Shape                                  = SimpleShape(name)
  def apply(name: String, params: Seq[Shape]): Shape              = ParameterizedShape(name, params)
  def apply(name: String, param: Shape, theRest: Shape*): Shape   = apply(name, param +: theRest.toSeq)
  def apply(name: String, param: String, theRest: String*): Shape = apply(name, (param +: theRest.toSeq).map(apply))

  def ? = ParamShape("?")

  def of[A: TypeTag]: Shape = {
    implicitly[TypeTag[A]].tpe match {
      case tr @ TypeRef(_, _, _) => forTypeRef(tr)
      case tr @ ExistentialType(_, typ) =>
        typ match {
          case ref: TypeRef => forTypeRef(ref)
          case _            => ParamShape(tr.toString)
        }

    }
  }

  private def forTypeRef(typeRef: TypeRef): Shape = {
    val TypeRef(x, sy, args) = typeRef
    if (args.isEmpty) {
//      val ts: universe.Symbol = x.termSymbol
//      println(s"""term symbol $ts
//                 |ts.isTerm => ${ts.isTerm}
//                 |ts.isAbstract => ${ts.isAbstract}
//                 |ts.isMethod => ${ts.isMethod}
//                 |ts.isParameter => ${ts.isParameter}
//                 |ts.isSpecialized => ${ts.isSpecialized}
//                 |ts.isStatic => ${ts.isStatic}
//                 |ts.isType => ${ts.isType}
//                 |
//             """.stripMargin)
      if (x.termSymbol.isTerm) {
        SimpleShape(sy.name.toString)
      } else {
        ParamShape(sy.name.toString)
      }
    } else {
      val params: List[Shape] = args.map {
        case other @ TypeRef(_, _, _) => forTypeRef(other)
        case tr @ ExistentialType(_, typ) =>
          typ match {
            case tr @ TypeRef(_, _, _) => forTypeRef(tr)
            case _                     => ParamShape(tr.toString)
          }
        case arg => SimpleShape(arg.toString)
      }
      ParameterizedShape(sy.name.toString, params)
    }
  }
}
