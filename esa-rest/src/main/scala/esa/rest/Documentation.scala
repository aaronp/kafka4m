package esa.rest

import endpoints.openapi
import endpoints.openapi.model.{Info, OpenApi, Schema}
import esa.endpoints.{CounterEndpoints, UserEndpoints}

/**
  * Generates OpenAPI documentation for the endpoints described in the `CounterEndpoints` trait.
  */
object Documentation //
    extends openapi.Endpoints       //
    with openapi.JsonSchemaEntities //
    with CounterEndpoints //
    with UserEndpoints {

  val api: OpenApi =
    openApi(
      Info(title = "API to manipulate a counter", version = "1.0.0")
    )(login, currentValue, increment)

  override implicit def optionalQueryStringParam[A](implicit evidence$1: Schema): Schema = ???

  override implicit def repeatedQueryStringParam[A, CC[X] <: Iterable](implicit evidence$2: Schema, factory: _root_.scala.collection.compat.Factory[A, CC[A]]): Schema = ???

  override def refineQueryStringParam[A, B](pa: Schema)(f: A => Option[B])(g: B => A): Schema = ???

  override def refineSegment[A, B](sa: Schema)(f: A => Option[B])(g: B => A): Schema = ???

  override def xmapRecord[A, B](record: _root_.esa.rest.Documentation.DocumentedJsonSchema.DocumentedRecord, f: A => B, g: B => A): _root_.esa.rest.Documentation.DocumentedJsonSchema.DocumentedRecord = ???

  override def xmapTagged[A, B](taggedA: _root_.esa.rest.Documentation.DocumentedJsonSchema.DocumentedCoProd, f: A => B, g: B => A): _root_.esa.rest.Documentation.DocumentedJsonSchema.DocumentedCoProd = ???

  override def xmapJsonSchema[A, B](jsonSchema: _root_.esa.rest.Documentation.DocumentedJsonSchema, f: A => B, g: B => A): _root_.esa.rest.Documentation.DocumentedJsonSchema = ???

  override implicit def uuidJsonSchema: _root_.esa.rest.Documentation.DocumentedJsonSchema = ???

  override implicit def mapJsonSchema[A](implicit jsonSchema: _root_.esa.rest.Documentation.DocumentedJsonSchema): _root_.esa.rest.Documentation.DocumentedJsonSchema = ???
}
