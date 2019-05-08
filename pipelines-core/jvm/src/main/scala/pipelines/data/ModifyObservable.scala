package pipelines.data

import java.nio.file.Path
import java.time
import java.time.ZonedDateTime

import io.circe.{Decoder, Encoder}
import monix.execution.Scheduler
import monix.execution.atomic.AtomicAny
import monix.reactive.Observable
import pipelines.core.Rate

import scala.reflect.ClassTag

/**
  * Represents a way to modify an observable -- an operation which ensures the observable is still the same type
  * (e.g. apply a rate-limit or some other side-effect)
  */
sealed trait ModifyObservable

trait ModifyTyped[A] extends ModifyObservable {
  def modify(input: Observable[A])(implicit sched: Scheduler): Observable[A]
}
trait ModifyAny extends ModifyObservable {
  def modify[A](input: Observable[A])(implicit sched: Scheduler): Observable[A]
}
trait ModifyMap[A, B] extends ModifyObservable {
  def modify(input: Observable[A])(implicit sched: Scheduler): Observable[B]
}

/**
  * A way to modify an observable
  */
abstract class ModifyObservableTyped[A: ClassTag] extends ModifyObservable {
  type T = A
  def tag: ClassTag[A]          = implicitly[ClassTag[A]]
  override def toString: String = s"${getClass.getSimpleName} for $name"
  def name: String              = asId[A]
  def modify(input: Observable[A])(implicit sched: Scheduler): Observable[A]
}

abstract class ModifyObservableAny extends ModifyObservable {
  override def toString: String = name
  def name: String              = asId(getClass)
  def modify[A: ClassTag](input: Observable[A])(implicit sched: Scheduler): Observable[A]
}

object ModifyObservable {
  case class Take[A: ClassTag](n: Int) extends ModifyObservableAny {
    override def modify[A: ClassTag](input: Observable[A])(implicit sched: Scheduler): Observable[A] = input.take(n)
  }

//  case class ModifyObservableJson[A : Encoder :Decoder :ClassTag](data :A) extends ModifyObservable {
//    override def modify(input: Observable[A])(implicit sched: Scheduler): Observable[A] = {
//???
//    }
//  }

  //
  //
//  case class Stats[A: ClassTag]() extends ModifyObservableTyped[A] {
//
//    override def applyTo(source: Observable[A]): Observable[StreamStatistics[A]] = {
//      import concurrent.duration._
//      source.bufferTimed(1.second).scan(StreamStatistics[A](None, 0, ZonedDateTime.now, 0, 0, ZonedDateTime.now)) {
//        case (stats, seq) =>
//          val now                 = ZonedDateTime.now
//          val diff: time.Duration = java.time.Duration.between(stats.connectedAt, now)
//          val total               = stats.totalReceived + seq.size
//          val avePerSecond = diff.getSeconds match {
//            case 0       => 0
//            case seconds => total / seconds
//          }
//          StreamStatistics[A](seq.lastOption, total, stats.connectedAt, seq.size, avePerSecond.toInt, now)
//      }
//    }
//  }

  case class Filter[A: ClassTag](filterVar: AtomicAny[A => Boolean]) extends ModifyObservableTyped[A] {
    override def modify(data: Observable[A])(implicit sched: Scheduler): Observable[A] = {
      data.flatMap { input =>
        val predicate = filterVar.get
        if (predicate(input)) {
          Observable(input)
        } else {
          Observable.empty
        }
      }
    }
  }
  object Filter {
    def apply[A: ClassTag](): Filter[A] = apply(_ => true)
    def apply[A: ClassTag](predicate: A => Boolean): Filter[A] = {
      new Filter(AtomicAny[A => Boolean](predicate))
    }
  }

  case class RateLimitAll(limitRef: AtomicAny[Rate]) extends ModifyObservableAny {
    override def modify[A: ClassTag](data: Observable[A])(implicit sched: Scheduler): Observable[A] = {
      data.bufferTimed(limitRef.get.per).map(select(_, limitRef.get.messages)).switchMap(Observable.fromIterable)
    }
  }
  object RateLimitAll {
    def apply(rate: Rate) = new RateLimitAll(AtomicAny[Rate](rate))
  }
  case class RateLimitLatest(limitRef: AtomicAny[Rate]) extends ModifyObservableAny {
    override def modify[A: ClassTag](data: Observable[A])(implicit sched: Scheduler): Observable[A] = {
      data.bufferTimed(limitRef.get.per).whileBusyDropEvents.map(select(_, limitRef.get.messages)).switchMap(Observable.fromIterable)
    }
  }
  object RateLimitLatest {
    def apply(rate: Rate) = new RateLimitLatest(AtomicAny[Rate](rate))
  }

  case class Persist(persistDir: Path) extends ModifyObservableTyped[Array[Byte]] {
    override def modify(input: Observable[Array[Byte]])(implicit sched: Scheduler): Observable[Array[Byte]] = {
      import eie.io._
      val dir = persistDir.mkDirs()
      input.zipWithIndex.map {
        case (d8a, id) =>
          dir.resolve(id.toString).bytes = d8a
          d8a
      }
    }
  }
}
