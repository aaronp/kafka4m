package pipelines.data

import pipelines.core.{ByteArray, DataType, JsonRecord}

/** A Functor -- wtf have I written?
  *
  * We need to kill this 'DataType' BS...s
  *
  * @tparam A
  */
trait TypeAdapter[-A] {
  type T
  def sourceType: DataType

  def map(input: A): T
}

object TypeAdapter {
  trait Aux {
    def orElse(other: Aux): Aux = {
      val parent = this
      new Aux {
        override def map[A](originalType: DataType, newType: DataType): Option[TypeAdapter[A]] = {
          val opt: Option[TypeAdapter[A]] = parent.map(originalType, newType)
          opt.orElse(other.map(originalType, newType))
        }
      }
    }

    def map[A](originalType: DataType, newType: DataType): Option[TypeAdapter[A]]
  }

  object Aux extends Default

  object JsonToBytes extends TypeAdapter[String] {
    override type T = Array[Byte]
    override def sourceType: DataType = ByteArray
    override def map(input: String) = {
      input.getBytes("UTF-8")
    }
  }

  object BytesToJson extends TypeAdapter[Array[Byte]] {
    override type T = String
    override def sourceType: DataType = JsonRecord
    override def map(input: Array[Byte]) = {
      new String(input, "UTF-8")
    }
  }

  trait Default extends Aux {
    override def map[A](originalType: DataType, newType: DataType): Option[TypeAdapter[A]] = {
      (originalType, newType) match {
        case (ByteArray, JsonRecord) => Option(BytesToJson.asInstanceOf[TypeAdapter[A]])
        case (JsonRecord, ByteArray) => Option(JsonToBytes.asInstanceOf[TypeAdapter[A]])
        case _                       => None
      }
    }
  }
}
