package pipelines.rest.openapi

import io.circe.{Encoder, Json, JsonNumber, JsonObject}

import scala.collection.compat.Factory

trait CirceAdapter extends endpoints.openapi.JsonSchemas {

  private def toMap(record: DocumentedJsonSchema.DocumentedRecord) = {
    record.fields.groupBy(_.name).mapValues {
      case List(only) => only
      case many       => sys.error(s"${many.size} fields found w/ the same name for $record : $many")
    }
  }

  def mergeFields(a: DocumentedJsonSchema.Field, b: DocumentedJsonSchema.Field) = {
    val doc = a.documentation.orElse(b.documentation)
    new DocumentedJsonSchema.Field(a.name, merge(a.tpe, b.tpe), isOptional = a.isOptional || b.isOptional, doc)
  }

  private def isNull(schema: DocumentedJsonSchema): Boolean = {
    schema match {
      case DocumentedJsonSchema.DocumentedRecord(fields, _, _) => fields.isEmpty
      case _                                                   => false
    }
  }
  def merge(a: DocumentedJsonSchema, b: DocumentedJsonSchema): DocumentedJsonSchema = {
    (a, b) match {
      case (lhs: DocumentedJsonSchema.DocumentedRecord, rhs: DocumentedJsonSchema.DocumentedRecord) =>
        val leftFields  = toMap(lhs)
        val rightFields = toMap(rhs)
        val mergedFields = (leftFields.keySet ++ rightFields.keySet).map { name =>
          (leftFields.get(name), rightFields.get(name)) match {
            case (Some(f1), Some(f2)) => mergeFields(f1, f2)
            case (Some(f1), None)     => f1
            case (None, Some(f2))     => f2
            case other                => sys.error(s"bug: $other")
          }
        }
        val additionalProperties = lhs.additionalProperties.orElse(rhs.additionalProperties)
        val name                 = lhs.name.orElse(rhs.name)
        DocumentedJsonSchema.DocumentedRecord(mergedFields.toList, additionalProperties, name)
      case _ =>
        // TODO
        a
    }
  }

  private class Folder extends Json.Folder[DocumentedJsonSchema] {
    override def onNull: DocumentedJsonSchema = emptyRecord

    override def onBoolean(value: Boolean): DocumentedJsonSchema = booleanJsonSchema

    override def onNumber(value: JsonNumber): DocumentedJsonSchema = {
      value.toInt
        .map(_ => intJsonSchema)
        .orElse(value.toLong.map(_ => longJsonSchema))
        .orElse(Option(value.toDouble).filterNot(_.isInfinite).map(_ => doubleJsonSchema))
        .getOrElse(bigdecimalJsonSchema)
    }

    override def onString(value: String): DocumentedJsonSchema = stringJsonSchema

    override def onArray(value: Vector[Json]): DocumentedJsonSchema = {
      val jsonValues: Seq[DocumentedJsonSchema] = value.map { json =>
        json.foldWith[DocumentedJsonSchema](this)
      }
      val f = implicitly[Factory[DocumentedJsonSchema, Seq[DocumentedJsonSchema]]]
      if (jsonValues.isEmpty) {
        arrayJsonSchema(emptyRecord, f)
      } else {
        val schema = jsonValues.reduce(merge)
        arrayJsonSchema(schema, f)
      }
    }

    override def onObject(value: JsonObject): DocumentedJsonSchema = {
      val byName = value.toMap
      if (byName.isEmpty) {
        emptyRecord
      } else {
        val fields = byName.map {
          case (key, json) =>
            val doc: DocumentedJsonSchema = json.foldWith[DocumentedJsonSchema](this)
            val optional                  = isNull(doc)
            DocumentedJsonSchema.Field(key, doc, optional, None)
        }
        DocumentedJsonSchema.DocumentedRecord(fields.toList)
      }
    }
  }

  def document(example: Json): DocumentedJsonSchema = {
    example.foldWith(new Folder)
  }

  def document[A: Encoder](example: A, theRest: A*): DocumentedJsonSchema = {
    import io.circe.syntax._

    val first = document(example.asJson)
    if (theRest.nonEmpty) {
      theRest.map(x => document(x)).foldLeft(first)(merge(_, _))
    } else {
      first
    }
  }
}
