package com.malliina.oauth

import com.malliina.values.{AccessToken, IdToken}
import io.circe.Codec

case class TokenResponse(
  access_token: AccessToken,
  id_token: IdToken,
  expires_in: Long,
  token_type: String
) derives Codec.AsObject
