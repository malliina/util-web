package com.malliina.http

import cats.data.NonEmptyList
import com.malliina.http.{Errors, SingleError}
import com.malliina.values.ErrorMessage
import io.circe.{Codec, Decoder, Encoder}

case class SingleError(message: ErrorMessage, key: String) derives Codec.AsObject

object SingleError:
  def apply(message: String): SingleError = apply(ErrorMessage(message), "generic")
  def input(message: String) = apply(ErrorMessage(message), "input")

case class Errors(errors: NonEmptyList[SingleError]) derives Codec.AsObject:
  def message = errors.head.message

object Errors:
  given [T: Codec]: Codec[NonEmptyList[T]] = Codec.from(
    Decoder.decodeNonEmptyList[T],
    Encoder.encodeNonEmptyList[T]
  )

  def apply(error: SingleError): Errors = Errors(NonEmptyList.of(error))
  def apply(message: ErrorMessage): Errors = Errors.single(message.message)
  def apply(message: String): Errors = apply(ErrorMessage(message))
  def single(message: String): Errors = Errors(NonEmptyList.of(SingleError(message)))
