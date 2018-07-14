package com.malliina.oauth

import com.malliina.values.Email
import play.api.libs.json.Json

case class IdToken(iss: String, sub: String, email: Email, aud: String, iat: Long, exp: Long)

object IdToken {
  implicit val idFormat = Json.format[IdToken]
}
