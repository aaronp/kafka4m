package pipelines.data

import java.nio.file.Path

import pipelines.core.StreamStrategy

/**
  * A bridge between a json-serializable [[ModifyObservableRequest]] and an [[ModifyObservable]] instance
  */
trait ModifyObservableResolver {
  def resolve(request: ModifyObservableRequest): Option[ModifyObservable]

  def update[A](request: ModifyObservable, dataSource: DataSource[A]): Option[DataSource[A]] = {
    dataSource.enrichments.find(_.getClass == request.getClass).flatMap { updated =>
      (request, updated) match {
        case (old: ModifyObservable.Filter[A], updated: ModifyObservable.Filter[A])             => updateFilter(old, updated, dataSource)
        case (old: ModifyObservable.RateLimitLatest, updated: ModifyObservable.RateLimitLatest) => updateRateLimitLatest(old, updated, dataSource)
        case (old: ModifyObservable.RateLimitAll, updated: ModifyObservable.RateLimitAll)       => updateRateLimitAll(old, updated, dataSource)
        case _                                                                                  => None
      }
    }
  }

  protected def updateFilter[A](old: ModifyObservable.Filter[A], updated: ModifyObservable.Filter[A], dataSource: DataSource[A]): Option[DataSource[A]] = {
    old.filterVar := updated.filterVar.get()
    Option(dataSource)
  }
  protected def updateRateLimitLatest[A](old: ModifyObservable.RateLimitLatest, updated: ModifyObservable.RateLimitLatest, dataSource: DataSource[A]): Option[DataSource[A]] = {
    old.limitRef := updated.limitRef.get()
    Option(dataSource)
  }
  protected def updateRateLimitAll[A](old: ModifyObservable.RateLimitAll, updated: ModifyObservable.RateLimitAll, dataSource: DataSource[A]): Option[DataSource[A]] = {
    old.limitRef := updated.limitRef.get()
    Option(dataSource)
  }

  final def orElse(other: ModifyObservableResolver): ModifyObservableResolver = {
    val parent = this
    new ModifyObservableResolver {
      override def resolve(request: ModifyObservableRequest): Option[ModifyObservable] = {
        other.resolve(request).orElse(parent.resolve(request))
      }

      override def update[A](request: ModifyObservable, dataSource: DataSource[A]): Option[DataSource[A]] = {
        other.update(request, dataSource).orElse(parent.update(request, dataSource))
      }
    }
  }
}

object ModifyObservableResolver {
  def apply() = new Base

  class Base extends ModifyObservableResolver {
    override def resolve(request: ModifyObservableRequest): Option[ModifyObservable] = {
      request match {
        case ModifyObservableRequest.Take(limit)                               => Option(ModifyObservable.Take(limit))
        case ModifyObservableRequest.RateLimit(newRate, StreamStrategy.All)    => Option(ModifyObservable.RateLimitAll(newRate))
        case ModifyObservableRequest.RateLimit(newRate, StreamStrategy.Latest) => Option(ModifyObservable.RateLimitLatest(newRate))
        case _                                                                 => None
      }
    }
  }

  case class Default(persistDir: Path) extends Base {
    override def resolve(request: ModifyObservableRequest): Option[ModifyObservable] = {
      super.resolve(request).orElse {
        request match {
          case ModifyObservableRequest.Persist(path) => Option(ModifyObservable.Persist(persistDir.resolve(path)))
          case _                                     => None
        }
      }
    }
  }
}
