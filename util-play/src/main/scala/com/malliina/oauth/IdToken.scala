package com.malliina.oauth

import play.api.libs.json.Json

/**
 * @author Michael
 */
case class IdToken(iss: String, sub: String, email: String, aud: String, iat: Long, exp: Long)

object IdToken {
  implicit val idFormat = Json.format[IdToken]
}
