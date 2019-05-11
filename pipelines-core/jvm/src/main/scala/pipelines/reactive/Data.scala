package pipelines.reactive

import monix.reactive.Observable

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

  def of(contentType: ContentType, obs: Observable[_]) = new AnonTypeData(contentType, obs)

  def apply[T: TypeTag](observable: Observable[T]): Data = {
    apply(ContentType.of[T], observable)
  }

  case class AnonTypeData(override val contentType: ContentType, observable: Observable[_]) extends Data {
    override def data(ct: ContentType): Option[Observable[_]] = {
      if (ct == contentType) {
        Option(observable)
      } else {
        None
      }
    }
  }

  case class SingleTypeData(override val contentType: ContentType, observable: Observable[_]) extends Data {
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

  def apply[T](contentType: ContentType, observable: Observable[T]) = {
    new SingleTypeData(contentType, observable)
  }
}
