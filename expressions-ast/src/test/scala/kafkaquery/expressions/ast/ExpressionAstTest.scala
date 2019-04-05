package kafkaquery.expresssions.ast

import fastparse.{Parsed, _}
import kafkaquery.expresssions.ast.ExpressionAst.PropertyPath
import org.scalatest.{Matchers, WordSpec}

class ExpressionAstTest extends WordSpec with Matchers {

  "ExpressionAst.evalExpr" ignore {

    import kafkaquery.expresssions.ast.ExpressionAst.ast._
    "parse op expressions" in {
      val result = parse("4.56 + 7.12", evalExpr(_)).get.value
      println(result)
      parse("4.56+7", full(_)).get.value shouldBe EvalExpression(ConstExpression(Right(DoubleValue(4.56))), Plus, ConstExpression(Right(DoubleValue(7))))
//      parse("true - false", full(_)).get.value shouldBe EvalExpression(ConstExpression(Right(DoubleValue(4.56))), Plus, ConstExpression(Right(DoubleValue(7))))

    }
  }
  "parse" ignore {

    import ExpressionAst.ast._
    "parse term expressions" in {
      parse("'foo.bar", full(_)).get.value shouldBe ConstExpression(Left(PropertyPath(List("foo", "bar"))))
      parse("123", full(_)).get.value shouldBe ConstExpression(Right(IntValue(123)))
      parse("true", full(_)).get.value shouldBe ConstExpression(Right(BooleanValue(true)))
      parse("4.56", full(_)).get.value shouldBe ConstExpression(Right(DoubleValue(4.56)))
    }

    "parse op expressions" in {
      parse("4.56 + 7", full(_)).get.value shouldBe EvalExpression(ConstExpression(Right(DoubleValue(4.56))), Plus, ConstExpression(Right(DoubleValue(7))))
      parse("4.56+7", full(_)).get.value shouldBe EvalExpression(ConstExpression(Right(DoubleValue(4.56))), Plus, ConstExpression(Right(DoubleValue(7))))
      parse("true - false", full(_)).get.value shouldBe EvalExpression(ConstExpression(Right(DoubleValue(4.56))), Plus, ConstExpression(Right(DoubleValue(7))))

    }
//    "parse grouped expressions" in {}
//    "parse predicate expressions" in {}
  }
  "ExpressionAst.ast.term" should {
    import ExpressionAst.ast._
    "parse properties" in {
      val Parsed.Success(Left(property), _) = parse("'path.to.a.property", term(_))
      property shouldBe PropertyPath(List("path", "to", "a", "property"))
    }
    "parse values" in {
      val Parsed.Success(Right(BooleanValue(f)), _) = parse("false", term(_))
      f shouldBe false
    }
  }

  "ExpressionAst.comparison" should {
    import ExpressionAst.comparison._
    "parse greater than or equal to" in {
      parse(">=", aComparison(_)).get.value shouldBe Gte
      parse("GTE", aComparison(_)).get.value shouldBe Gte
      parse("GTE", aComparison(_)).get.value shouldBe Gte
    }
    "parse greater than" in {
      parse(">", aComparison(_)).get.value shouldBe Gt
      parse("GT", aComparison(_)).get.value shouldBe Gt
      parse("gt", aComparison(_)).get.value shouldBe Gt
    }
    "parse less than or equal to" in {
      parse("<=", aComparison(_)).get.value shouldBe Lte
      parse("lte", aComparison(_)).get.value shouldBe Lte
      parse("LTE", aComparison(_)).get.value shouldBe Lte
    }
    "parse less than" in {
      parse("<", aComparison(_)).get.value shouldBe Lt
      parse("lt", aComparison(_)).get.value shouldBe Lt
      parse("LT", aComparison(_)).get.value shouldBe Lt
    }
    "parse not equal to" in {
      parse("!=", aComparison(_)).get.value shouldBe NotEqualTo
      parse("neq", aComparison(_)).get.value shouldBe NotEqualTo
      parse("NEQ", aComparison(_)).get.value shouldBe NotEqualTo
    }
    "parse equal to" in {
      parse("==", aComparison(_)).get.value shouldBe EqualTo
      parse("eq", aComparison(_)).get.value shouldBe EqualTo
      parse("EQ", aComparison(_)).get.value shouldBe EqualTo
    }
  }
  "ExpressionAst.op" should {
    import ExpressionAst.op._
    "parse plus" in {
      parse("+", anOp(_)).get.value shouldBe Plus
      parse("plus", anOp(_)).get.value shouldBe Plus
      parse("PLUS", anOp(_)).get.value shouldBe Plus
    }
    "parse minus" in {
      parse("-", anOp(_)).get.value shouldBe Minus
      parse("minus", anOp(_)).get.value shouldBe Minus
      parse("MINUS", anOp(_)).get.value shouldBe Minus
    }
    "parse times" in {
      parse("*", anOp(_)).get.value shouldBe Times
      parse("x", anOp(_)).get.value shouldBe Times
      parse("X", anOp(_)).get.value shouldBe Times
      parse("times", anOp(_)).get.value shouldBe Times
      parse("TIMES", anOp(_)).get.value shouldBe Times
    }
    "parse div" in {
      parse("/", anOp(_)).get.value shouldBe DividedBy
      parse("div", anOp(_)).get.value shouldBe DividedBy
      parse("DIV", anOp(_)).get.value shouldBe DividedBy
    }
    "parse mod" in {
      parse("%", anOp(_)).get.value shouldBe Modulo
      parse("mod", anOp(_)).get.value shouldBe Modulo
      parse("MOD", anOp(_)).get.value shouldBe Modulo
    }
  }
  "ExpressionAst.ast.text" should {
    import ExpressionAst.ast._
    "parse single quoted text" in {
      parse("""'single'""", text(_)) shouldBe Parsed.Success("single", 8)
    }
    "parse quoted text" in {
      parse("\"quoted\"", text(_)) shouldBe Parsed.Success("quoted", 8)
    }
    "parse identifiers" in {
      parse("foobar", text(_)) shouldBe Parsed.Success("foobar", 6)
    }
  }
  "ExpressionAst.ast.value" should {
    "parse long values" in {
      val Parsed.Success(LongValue(max), _) = parse(Long.MaxValue.toString, ExpressionAst.ast.value(_))
      max shouldBe Long.MaxValue

      val Parsed.Success(LongValue(min), _) = parse(Long.MinValue.toString, ExpressionAst.ast.value(_))
      min shouldBe Long.MinValue
    }
    "parse int values" in {
      val Parsed.Success(IntValue(max), _) = parse(Int.MaxValue.toString, ExpressionAst.ast.value(_))
      max shouldBe Int.MaxValue

      val Parsed.Success(IntValue(min), _) = parse(Int.MinValue.toString, ExpressionAst.ast.value(_))
      min shouldBe Int.MinValue

      val Parsed.Success(IntValue(value), _) = parse("0", ExpressionAst.ast.value(_))
      value shouldBe 0
    }
    "parse doubles" in {
      val Parsed.Success(DoubleValue(max), _) = parse("1.7976931348623157", ExpressionAst.ast.value(_))
      max shouldBe 1.7976931348623157

      val Parsed.Success(DoubleValue(min), _) = parse("-1.7976931348623157", ExpressionAst.ast.value(_))
      min shouldBe -1.7976931348623157

      val Parsed.Success(DoubleValue(dbl), _) = parse("12.34", ExpressionAst.ast.value(_))
      dbl shouldBe 12.34
    }
    "parse booleans" in {
      parse("TRUE", ExpressionAst.ast.value(_)).get.value shouldBe BooleanValue(true)
      parse("true", ExpressionAst.ast.value(_)).get.value shouldBe BooleanValue(true)
      parse("false", ExpressionAst.ast.value(_)).get.value shouldBe BooleanValue(false)
      parse("FALSE", ExpressionAst.ast.value(_)).get.value shouldBe BooleanValue(false)
    }
  }
}
