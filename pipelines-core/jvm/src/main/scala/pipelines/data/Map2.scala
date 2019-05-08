package pipelines.data

import cats.Functor
import monix.reactive.Observable

import scala.util.Try

/**
  * A functor but stripping the type declarations
  */
trait Map2 {
  type H[A]
  def name: String
  def cast[A](owt: Any): H[A]
  def map2[A, B](fa: H[A])(f: A => B): H[B]
  def asCompute[A](liftToFA: Compute, next: Compute): Compute = {
    def applyMap2(input: Any): Any = {
      val shouldBeFA = liftToFA.applyUnsafe(input)
      val fa: H[A]   = cast(shouldBeFA)
      map2(fa)(next.applyUnsafe)
    }
    Compute.Lift(s => ParameterizedShape(name, Seq(s)), s"${name}[_]", applyMap2 _)
  }
}

object Map2 {

  def values: Seq[Map2] = {
    Seq[Map2](
      OptionFunctor,
      TryFunctor,
      ListFunctor,
      VectorFunctor,
      ObservableFunctor
    )
  }

  object OptionFunctor extends Functor[Option] with Map2 {
    type H[A] = Option[A]
    override def map[A, B](fa: Option[A])(f: A => B): Option[B] = fa.map(f)
    override def map2[A, B](fa: Option[A])(f: A => B)           = fa.map(f)
    override def cast[A](owt: Any)                              = owt.asInstanceOf[Option[A]]

    override def name: String = "Option"
  }
  object TryFunctor extends Functor[Try] with Map2 {
    override def name: String = "Try"
    type H[A] = Try[A]
    override def map[A, B](fa: Try[A])(f: A => B): Try[B] = fa.map(f)
    override def map2[A, B](fa: Try[A])(f: A => B)        = fa.map(f)
    override def cast[A](owt: Any)                        = owt.asInstanceOf[Try[A]]
  }
  object ListFunctor extends Functor[List] with Map2 {
    override def name: String = "List"
    type H[A] = List[A]
    override def map[A, B](fa: List[A])(f: A => B): List[B] = fa.map(f)
    override def map2[A, B](fa: List[A])(f: A => B)         = fa.map(f)
    override def cast[A](owt: Any)                          = owt.asInstanceOf[List[A]]
  }
  object VectorFunctor extends Functor[Vector] with Map2 {
    override def name: String = "Vector"
    type H[A] = Vector[A]
    override def map[A, B](fa: Vector[A])(f: A => B): Vector[B] = fa.map(f)
    override def map2[A, B](fa: Vector[A])(f: A => B)           = fa.map(f)
    override def cast[A](owt: Any)                              = owt.asInstanceOf[Vector[A]]
  }
  object ObservableFunctor extends Functor[Observable] with Map2 {
    override def name: String = "Observable"
    type H[A] = Observable[A]
    override def map[A, B](fa: Observable[A])(f: A => B): Observable[B] = fa.map(f)
    override def map2[A, B](fa: Observable[A])(f: A => B)               = fa.map(f)
    override def cast[A](owt: Any)                                      = owt.asInstanceOf[Observable[A]]
  }
}
