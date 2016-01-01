package com.malliina.oauth

import play.api.libs.json.Json

/**
 * @author Michael
 */
case class TokenResponse(access_token: String, id_token: String, expires_in: Long, token_type: String)

object TokenResponse {
  implicit val tokenFormat = Json.format[TokenResponse]
}
