package pipelines.expresssions.ast

import fastparse._
import NoWhitespace._

/**
  * term := <prop> | <value>
  * value := text | number | boolean
  * number := long | double
  * text := "quoted" | 'quoted' | no-Whitespace
  * prop := 'a.b.c # jpath  or xpath
  * predicate := <boolean> | <expr> <comparison> <expr> | <predicate> <andOr> <predicate> | ( <predicate> <andOr> <predicate> )
  * comparison := gt | gte | lt | lte | eq | < | > | <= | >= | != | ne
  * expr : <term> | <predicate> | <expr> <op> <expr> | ( <expr> <op> <expr> )
  * op : plus | + | minus | - | div | / | times | X | * | mod | %
  */
object ExpressionAst {

  case class PropertyPath(parts: Seq[String])

  def parse(input: String): Parsed[Expression] = {
    fastparse.parse(input, ast.full(_))
  }

  object ast {
    def full[_: P]: P[Expression] = expression ~ P(End)

    import support._

    def text[_: P]: P[String] = quoted | singleQuoted | anIdentifier

    def value[_: P]: P[Value] = {
      aDouble.map(DoubleValue.apply) | aLong.map(LongValue.apply).map(_.normalize()) | aBoolean.map(BooleanValue.apply) | text.map(TextValue.apply)
    }

    private def subProperty[_: P] = aDot ~ anIdentifier

    def property[_: P]: P[PropertyPath] = {

      val parser: P[(String, Seq[String])] = aTick ~ (anIdentifier ~ subProperty.rep(0))
      parser.map {
        case (head, tail) => PropertyPath(head +: tail)
      }
    }

    def term[_: P]: P[Term] = property.map(Left.apply) | value.map(Right.apply)

    def expressionOld[_: P]: P[Expression] = evalExpr | term.map(ConstExpression.apply) | groupedExpr

    def theRest[_: P] = {
      (whitespace(0) ~ op.anOp ~ whitespace(0) ~ expression).rep(0)
    }

    def repeatingExpression[_: P] = {
      term.map(ConstExpression.apply) // ~
    }

    def expression[_: P]: P[Expression] = {
      groupedExpr // |
    }
//    def expression[_: P]: P[Expression] = term.map(ConstExpression.apply) | groupedExpr | evalExpr

    def groupedExpr[_: P]: P[Expression] = (P("(") ~ whitespace(0) ~ evalExpr ~ whitespace(0) ~ P(")"))

    def test[_: P] = {
      support.aDouble ~ whitespace(0) ~ op.anOp ~ whitespace(0) ~ support.aDouble
    }

    def evalExpr[_: P]: P[Expression] = {
      (expression ~ whitespace(0) ~ op.anOp ~ whitespace(0) ~ expression).map {
        case (lhs, operation, rhs) => EvalExpression(lhs, operation, rhs)
      }
    }

    def predicate[_: P]: P[Predicate] = groupedPredicate | conjunctionPredicate | predicateEval | aBoolean.map(ConstPredicate.apply)

    private def groupedPredicate[_: P]: P[Predicate] = {
      (P("(") ~ whitespace(0) ~ conjunctionPredicate ~ whitespace(0) ~ P(")"))
    }

    private def predicateEval[_: P]: P[EvalPredicate] = {
      expression.flatMap { lhs =>
        whitespace(0).flatMap { _ =>
          comparison.aComparison.flatMap { comp =>
            whitespace(0).flatMap { _ =>
              expression.map { rhs =>
                EvalPredicate(lhs, comp, rhs)
              }
            }
          }
        }
      }
    }

    private def conjunctionPredicate[_: P]: P[ConjunctionPredicate] = {
      predicate.flatMap { lhs =>
        someWhitespace.flatMap { _ =>
          conjunction.aConjunction.flatMap { andOr =>
            someWhitespace.flatMap { _ =>
              predicate.map { rhs =>
                ConjunctionPredicate(lhs, andOr, rhs)
              }
            }
          }
        }
      }
    }
  }

  object comparison {
    def aComparison[_: P]: P[Comparison] = eq | neq | lte | lt | gte | gt

    def gte[_: P] = P(">=" | IgnoreCase("gte")).map(_ => Gte)

    def gt[_: P] = P(">" | IgnoreCase("gt")).map(_ => Gt)

    def lte[_: P] = P("<=" | IgnoreCase("lte")).map(_ => Lte)

    def lt[_: P] = P("<" | IgnoreCase("lt")).map(_ => Lt)

    def eq[_: P] = P("==" | IgnoreCase("eq")).map(_ => EqualTo)

    def neq[_: P] = P("!=" | IgnoreCase("neq")).map(_ => NotEqualTo)
  }

  object conjunction {
    def aConjunction[_: P]: P[AndOr] = and | or

    def and[_: P] = P(IgnoreCase("And") | "&".rep(1, max = 2)).map(_ => And)

    def or[_: P] = P(IgnoreCase("Or") | "|".rep(1, max = 2)).map(_ => Or)
  }

  object op {

    def anOp[_: P]: P[Op] = {
      aPlus | aMinus | aTimes | aDiv | aMod
    }

    def aPlus[_: P] = P("+" | IgnoreCase("plus")).map(_ => Plus)

    def aMinus[_: P] = P("-" | IgnoreCase("minus")).map(_ => Minus)

    def aTimes[_: P] = P("*" | IgnoreCase("X") | IgnoreCase("times")).map(_ => Times)

    def aDiv[_: P] = P("/" | IgnoreCase("div")).map(_ => DividedBy)

    def aMod[_: P] = P("%" | IgnoreCase("mod")).map(_ => Modulo)
  }

  object support {

    /** string starting with a character and is all letters or numbers
      */
    def anIdentifier[_: P]: P[String] = (aLetter ~ CharsWhile(_.isLetterOrDigit, 0)).!

    def aLetter[_: P]: P[String] = CharPred(_.isLetter).!

    def aTick[_: P]: P[Unit] = CharPred(_ == '\'')

    def quoted[_: P]: P[String] = "\"" ~ CharPred(_ != '\"').rep.! ~ "\""

    def singleQuoted[_: P]: P[String] = "'" ~ CharPred(_ != '\'').rep.! ~ "'"

    def aDot[_: P]: P[Unit] = CharPred(_ == '.')

    def aLong[_: P]: P[Long] = ("-".? ~ CharsWhile(_.isDigit, 1)).!.map(_.toLong)

    def aBoolean[_: P]: P[Boolean] = (IgnoreCase("true") | IgnoreCase("false")).!.map(_.toBoolean)

    def aDouble[_: P]: P[Double] = ("-".? ~ CharsWhile(_.isDigit, 1) ~ aDot ~ CharsWhile(_.isDigit, 1)).!.map(_.toDouble)

    def whitespace[_: P](min: Int): P[Unit] = P(CharsWhile(_.isWhitespace).rep(min))

    def someWhitespace[_: P]: P[Unit] = whitespace(1)

  }

}
