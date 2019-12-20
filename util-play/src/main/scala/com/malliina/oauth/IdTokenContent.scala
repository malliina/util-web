package com.malliina.oauth

import com.malliina.values.Email
import play.api.libs.json.Json

case class IdTokenContent(iss: String, sub: String, email: Email, aud: String, iat: Long, exp: Long)

object IdTokenContent {
  implicit val json = Json.format[IdTokenContent]
}
