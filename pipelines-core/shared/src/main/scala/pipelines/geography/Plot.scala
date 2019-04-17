package pipelines.geography

import scala.compat.Platform

object Plot {

  def mergeMap[K, V](m1: Map[K, V], m2: Map[K, V])(merge: (V, V) => V): Map[K, V] = {
    val k1      = m1.keys
    val k2      = m2.keys
    val allKeys = k1 ++ k2
    allKeys.map { key =>
      val row = (m1.get(key), m2.get(key)) match {
        case (Some(lhs), Some(rhs)) => merge(lhs, rhs)
        case (Some(lhs), None)      => lhs
        case (None, Some(rhs))      => rhs
        case (None, None)           => sys.error("broken")
      }
      key -> row
    }.toMap
  }

  case class AsciiRow(charsAtX: List[(Int, Char)] = Nil) {
    def this(x: Int, c: Char) = this(List(x -> c))

    def x1 = charsAtX.headOption.map(_ => rowByXCoord.keySet.min).getOrElse(0)
    def x2 = charsAtX.headOption.map(_ => rowByXCoord.keySet.max).getOrElse(0)

    def width = x2 - x1

    lazy val rowByXCoord: Map[Int, Char] = charsAtX.toMap.ensuring(_.size == charsAtX.size)
    def render(minX: Int, space: Char = ' '): String = {
      val lookup = charsAtX.toMap.withDefault(_ => space)
      (minX to x2).map(lookup).mkString("")
    }

    def at(x: Int) = rowByXCoord.get(x)

    def merge(other: AsciiRow, intersectChar: Option[Char]): AsciiRow = {
      val newChars = mergeMap(rowByXCoord, other.rowByXCoord) {
        case (_, rhs) => intersectChar.getOrElse(rhs)
      }
      AsciiRow(newChars.toList)
    }
  }

  case class AsciiRows(rows: List[(Int, AsciiRow)] = Nil) {
    def width = rows.map(_._2.width).reduceOption(_ max _).getOrElse(0)

    def height: Int = y1 - y2

    def y1 = rows.headOption.map(_ => rowByYCoord.keySet.max).getOrElse(0)
    def y2 = rows.headOption.map(_ => rowByYCoord.keySet.min).getOrElse(0)

    def minX = rows.map(_._2.x1).reduceOption(_ min _).getOrElse(0)

    lazy val rowByYCoord  = rows.toMap.ensuring(_.size == rows.size)
    override def toString = render()

    def render(space: Char = ' '): String = {
      val cachedMinX = minX
      val asciiRows  = rowByYCoord.mapValues(_.render(cachedMinX, space)).withDefault(_ => "")
      (y1 to y2 by -1).map(asciiRows).mkString(Platform.EOL)
    }

    def at(y: Int) = rowByYCoord.get(y)

    def merge(other: AsciiRows, intersectChar: Option[Char] = None): AsciiRows = {
      val newRows = mergeMap(rowByYCoord, other.rowByYCoord) {
        case (lhs, rhs) => lhs.merge(rhs, intersectChar)
      }
      AsciiRows(newRows.toList.sortBy(_._1))
    }
  }

  trait Layout[T] {
    type UI

    def layout(value: T, view: Rectangle): UI
  }

  object Layout {

    type Aux[T, UI2] = Layout[T] { type UI = UI2 }

    implicit def auxAsLayout[T, UI2](implicit aux: Aux[T, UI2]): Layout[T] { type UI = UI2 } = {
      aux
    }

    object syntax {
      implicit def richValue[T](value: T) = new {
        def layout[UI](view: Rectangle)(implicit aux: Aux[T, UI]): UI = {
          aux.layout(value, view)
        }
      }
    }

    type AsciiLayout[T] = Aux[T, AsciiRows]

    class AsciiLine(char: Char) extends Layout[LineSegment] {
      type UI = AsciiRows

      override def layout(line: LineSegment, view: Rectangle): AsciiRows = {
        val lineBounds = line.boundingBox

        val rows = (view.y2.toInt to view.y1.toInt).filter(y => lineBounds.containsY(y)).flatMap { y =>
          if (line.slope.isHorizontal) {
            // if we're on the same y point, then it's all the x points
            if (line.b.toInt == y) {
              val (fromX, toX) = {
                val a = line.x1.toInt
                val b = line.x2.toInt
                if (a < b) (a, b) else (b, a)
              }
              val points = (fromX to toX).filter(x => view.containsX(x)).map(_ -> char).toList
              Option(y -> AsciiRow(points))
            } else {
              None
            }
          } else {
            val xValue = line.xValueAt(y).toInt
            if (view.containsX(xValue) && lineBounds.containsX(xValue)) {
              Option(y -> new AsciiRow(xValue, char))
            } else {
              None
            }
          }
        }

        AsciiRows(rows.toList)
      }
    }
    class AsciiPoint(c : Char) extends Layout[Point] {
      type UI = AsciiRows
      override def layout(value: Point, view: Rectangle): AsciiRows = {
        val rows = if (view.contains(value)) {
          List(value.y.toInt -> AsciiRow(List(value.x.toInt -> c)))
        } else {
          Nil
        }
        AsciiRows(rows)
      }
    }
    implicit object AsciiPointImpl extends AsciiPoint('x')
    implicit object AsciiLineImpl extends AsciiLine('o')
    implicit object AsciiPolygon extends Layout[Polygon] {
      type UI = AsciiRows
      override def layout(value: Polygon, view: Rectangle): AsciiRows = {
        value.edges.map(AsciiLineImpl.layout(_, view)).reduce(_ merge _)
      }
    }
    implicit object AsciiRectangle extends Layout[Rectangle] {
      type UI = AsciiRows
      override def layout(value: Rectangle, view: Rectangle): AsciiRows = {
        value.edges.map(AsciiLineImpl.layout(_, view)).reduce(_ merge _)
      }
    }
  }
}
