package com.malliina.json

import play.api.libs.json._

abstract class ValidatingCompanion[In: Format, T] {
  private val reader = Reads[T] { json =>
    json.validate[In].flatMap(in => build(in)
      .map[JsResult[T]](t => JsSuccess(t))
      .getOrElse(JsError(invalidInputMessage(in))))
  }
  private val writer = Writes[T](t => Json.toJson(write(t)))
  implicit val json = Format[T](reader, writer)

  def build(input: In): Option[T]

  def write(t: T): In

  def invalidInputMessage(in: In): String = s"Invalid input: '$in'."
}
