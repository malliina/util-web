package com.malliina.oauth

import com.malliina.values.{AccessToken, IdToken}
import io.circe.*
import io.circe.generic.semiauto.*

case class TokenResponse(
  access_token: AccessToken,
  id_token: IdToken,
  expires_in: Long,
  token_type: String
)

object TokenResponse:
  implicit val json: Codec[TokenResponse] = deriveCodec[TokenResponse]
