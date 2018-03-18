package com.malliina.play.auth

import java.text.ParseException
import java.time.Instant

import com.malliina.http.{ResponseError, StatusError}
import play.api.libs.json.{JsError, JsPath, JsonValidationError}

import scala.concurrent.duration.{Duration, DurationLong}

sealed abstract class AuthError(val key: String) {
  def message: String
}

object JsonError {
  def apply(err: Seq[(JsPath, Seq[JsonValidationError])]): JsonError =
    apply(JsError(err))

  def apply(message: String): JsonError =
    JsonError(JsError(message))
}

case class OkError(error: ResponseError) extends AuthError("http_error") {
  override def message: String = error match {
    case StatusError(r, _) => s"Status code ${r.code}."
    case com.malliina.http.JsonError(_, _, _) => "JSON error."
    case _ => "Unknown error"
  }
}

case class PermissionError(message: String) extends AuthError("permission_error")

case class OAuthError(message: String) extends AuthError("oauth_error")

case class JsonError(err: JsError) extends AuthError("json_error") {
  override def message = s"JSON error. $err"
}

sealed abstract class JWTError(key: String) extends AuthError(key) {
  def token: TokenValue

  def message: String
}

case class Expired(token: TokenValue, exp: Instant, now: Instant)
  extends JWTError("token_expired") {
  def since: Duration = (now.toEpochMilli - exp.toEpochMilli).millis

  override def message = s"Token expired $since ago, at $exp."
}

case class NotYetValid(token: TokenValue, nbf: Instant, now: Instant)
  extends JWTError("not_yet_valid") {
  def validIn = (nbf.toEpochMilli - now.toEpochMilli).millis

  override def message = s"Token not yet valid. Valid in $validIn. Valid from $nbf, checked at $now."
}

case class IssuerMismatch(token: TokenValue, actual: String, allowed: Seq[String])
  extends JWTError("issuer_mismatch") {
  def message = s"Issuer mismatch. Got '$actual', but expected one of '${allowed.mkString(", ")}'."
}

case class InvalidSignature(token: TokenValue)
  extends JWTError("invalid_signature") {
  override def message = "Invalid JWT signature."
}

case class InvalidKeyId(token: TokenValue, kid: String, expected: Seq[String])
  extends JWTError("invalid_kid") {
  def message = s"Invalid key ID. Expected one of '${expected.mkString(", ")}', but got '$kid'."
}

case class InvalidClaims(token: TokenValue, message: String)
  extends JWTError("invalid_claims")

case class ParseError(token: TokenValue, e: ParseException)
  extends JWTError("parse_error") {
  override def message = "Parse error"
}

case class MissingData(token: TokenValue, message: String)
  extends JWTError("missing_data")
