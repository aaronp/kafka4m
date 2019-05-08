package pipelines.data

import pipelines.data.Compute.Composed

import scala.reflect.ClassTag

/**
  * Represents a computation of an output from an input
  */
trait Compute {

  type In

  /** @param input
    * @return the output types for the input types, if this calculation is defined for the given input
    */
  def outputFor(input: Shape): Option[Shape]

  def applyTo(input: Any): Any

  final def applyUnsafe(input: Any): Any = {
    applyTo(input)
  }
  final def andThen(other: Compute) = Composed(this, other)
  final def withName(name: String)  = NamedCompute(name, this)
}

object Compute {

  import scala.reflect.runtime.universe._
  def apply[In: TypeTag, Out: TypeTag](f: In => Out): Compute = {
    val in  = Shape.of[In]
    val out = Shape.of[Out]
    of(in, out)(f)
  }

  /** A function which will work when given input of the specific shape
    *
    * @param in the input type (shape)
    * @param f the thunk
    * @tparam A the input to the actual fuction
    * @return a compute which wil
    */
  def of[A <: Any](in: Shape, out: Shape)(f: A => Any): Compute = {
    val outOpt = Option(out)
    val desc   = s"$in -> ${out}"
    def typeMatch(check: Shape) = {
      if (check == in) {
        outOpt
      } else {
        None
      }
    }
    new Partial(typeMatch, desc, f)
  }

  /**
    * A function which is defined for any input. E.g. given a 'T' it will produce some F[T]]
    *
    * @param input
    * @param f
    * @tparam A
    * @return
    */
  def lift[A](input: Shape => Shape)(f: Any => Any): Compute = {
    val desc = s"Lift[${input(Shape.?)}]"
    new Lift(input, desc, f)
  }

  def partial(input: PartialFunction[Shape, Shape]) = PartialBuilder(input)

  def compose(f1: Compute, f2: Compute): Compute = Composed(f1, f2)

  case class Composed(first: Compute, andThen: Compute) extends Compute {
    override type In = Any

    /** @param input
      * @return the output types for the input types, if this calculation is defined for the given input
      */
    override def outputFor(input: Shape): Option[Shape] = {
      first.outputFor(input).flatMap(andThen.outputFor)
    }

    override def applyTo(input: In): Any = {
      andThen.applyUnsafe(first.applyUnsafe(input))
    }
  }

  /**
    * A normal function which can produce outputs for all its inputs
    *
    * @param calcOutput
    * @param desc
    * @param thunk
    */
  case class Lift(calcOutput: Shape => Shape, desc: String, thunk: Any => Any) extends Compute {
    override type In = Any
    override def applyTo(input: In) = thunk(input)
    override def outputFor(input: Shape): Option[Shape] = {
      Option(calcOutput(input))
    }
  }

  case class Partial[A <: Any, B](calcOutput: Shape => Option[Shape], desc: String, thunk: A => B) extends Compute {
    override def toString() = s"Partial[$desc]"
    override type In = A
    override def applyTo(input: Any) = {
      val ffs = thunk.asInstanceOf[Any => B]

      try {
        ffs(input)
      } catch {
        case exp => throw new Exception(s"$desc failed w/ $exp", exp)
      }
    }
    override def outputFor(input: Shape): Option[Shape] = {
      calcOutput(input)
    }
  }

  case class PartialBuilder(input: PartialFunction[Shape, Shape]) {
    def of[A: ClassTag, B: ClassTag](f: A => B): Compute = {
      val desc = s"${asId[A]} -> ${asId[B]}"
      new Partial[A, B](input.lift, desc, f)
    }
  }

}
