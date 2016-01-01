package com.malliina.play.json

import com.malliina.storage.{StorageLong, StorageSize}
import play.api.libs.json.Json._
import play.api.libs.json.{Format, JsResult, JsValue}

import scala.concurrent.duration.{Duration, DurationLong}

/**
 *
 * @author mle
 */
trait JsonFormats {

  /**
   * Serializes Duration to Long (seconds), deserializes Long to Duration.
   */
  implicit object durationFormat extends Format[Duration] {
    def writes(o: Duration): JsValue = toJson(o.toSeconds)

    def reads(json: JsValue): JsResult[Duration] =
      json.validate[Long].map(_.seconds)
  }

  implicit object storageSizeFormat extends Format[StorageSize] {
    override def writes(o: StorageSize): JsValue = toJson(o.toBytes)

    override def reads(json: JsValue): JsResult[StorageSize] =
      json.validate[Long].map(_.bytes)
  }

  /**
   * Json reader/writer. Writes toString and reads as specified by `f`.
   *
   * @param reader maps a name to the type
   * @tparam T type of element
   */
  class SimpleFormat[T](reader: String => T) extends Format[T] {
    def reads(json: JsValue): JsResult[T] =
      json.validate[String].map(reader)

    def writes(o: T): JsValue = toJson(o.toString)
  }

}

object JsonFormats extends JsonFormats
