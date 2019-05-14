package pipelines.reactive

import monix.execution.{Ack, Scheduler}
import monix.reactive.{Observable, Observer, Pipe}

import scala.concurrent.Future

/**
  * Represents a data source -- some type coupled with a means of consuming that data
  */
trait Data {

  /** @return the content type of this data source
    */
  def contentType: ContentType
  def data(ct: ContentType): Option[Observable[_]]
  def asObservable: Observable[_] = {
    data(contentType).getOrElse {
      sys.error(s"${this} wasn't able to provide an observable for its own content type '$contentType'")
    }
  }
}

object Data {
  import scala.reflect.runtime.universe._

  def of(contentType: ContentType, obs: Observable[_]): Data = new AnonTypeData(contentType, obs)

  def apply[T: TypeTag](observable: Observable[T]): Data = {
    apply(ContentType.of[T], observable)
  }

  def apply[T](contentType: ContentType, observable: Observable[T]): Data = {
    new SingleTypeData(contentType, observable)
  }

  def push[A: TypeTag](implicit sched: Scheduler): PushSource[A] = {
    val (input: Observer[A], output: Observable[A]) = Pipe.publish[A].multicast
    new PushSource[A](ContentType.of[A], input, output)
  }

  def createPush[A: TypeTag]: NewSource = createPush[A](ContentType.of[A])

  def createPush[A](contentType: ContentType): NewSource = {
    NewSource { implicit sched =>
      push[A](contentType)
    }
  }

  def push[A](contentType: ContentType)(implicit sched: Scheduler): PushSource[A] = {
    val (input: Observer[A], output: Observable[A]) = Pipe.publish[A].multicast
    new PushSource[A](contentType, input, output)
  }

  class PushSource[A](override val contentType: ContentType, val input: Observer[A], obs: Observable[A]) extends Data {
    def push(value: A): Future[Ack] = input.onNext(value)
    override def data(ct: ContentType): Option[Observable[_]] = {
      if (ct == contentType) {
        Option(obs)
      } else {
        None
      }
    }
  }

  private case class AnonTypeData(override val contentType: ContentType, observable: Observable[_]) extends Data {
    override def data(ct: ContentType): Option[Observable[_]] = {
      if (ct == contentType) {
        Option(observable)
      } else {
        None
      }
    }
  }

  private case class SingleTypeData(override val contentType: ContentType, observable: Observable[_]) extends Data {
    override def data(ct: ContentType): Option[Observable[Any]] = {
      if (contentType == ct) {
        Option(observable)
      } else {
        contentType match {
          case ClassType("Tuple2", Seq(t1, t2)) =>
            observable match {
              case x: Observable[(_, _)] if t1 == ct => Option(x.map(_._1))
              case x: Observable[(_, _)] if t2 == ct => Option(x.map(_._2))
              case _                                 => None
            }
          case _ => None
        }
      }
    }
  }
}
