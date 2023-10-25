package com.malliina.oauth

import com.malliina.http.FullUrl
import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Codec, Decoder}

case class GoogleOAuthJson(
  issuer: String,
  authorization_endpoint: FullUrl,
  token_endpoint: FullUrl,
  userinfo_endpoint: String,
  revocation_endpoint: String,
  jwks_uri: String,
  response_types_supported: Seq[String],
  subject_types_supported: Seq[String],
  id_token_signing_alg_values_supported: Seq[String],
  scopes_supported: Seq[String],
  token_endpoint_auth_methods_supported: Seq[String],
  claims_supported: Seq[String]
) derives Codec.AsObject:
  def canonical = GoogleOAuthConf(
    issuer,
    authorization_endpoint,
    token_endpoint,
    userinfo_endpoint,
    revocation_endpoint,
    jwks_uri,
    response_types_supported,
    subject_types_supported,
    id_token_signing_alg_values_supported,
    scopes_supported,
    token_endpoint_auth_methods_supported,
    claims_supported
  )

case class GoogleOAuthConf(
  issuer: String,
  authorizationEndpoint: FullUrl,
  tokenEndpoint: FullUrl,
  userInfoEndpoint: String,
  revocationEndpoint: String,
  jwksUri: String,
  responseTypesSupported: Seq[String],
  subjectTypesSupported: Seq[String],
  algorithmsSupported: Seq[String],
  scopesSupported: Seq[String],
  authMethodsSupported: Seq[String],
  claimsSupported: Seq[String]
)

object GoogleOAuthConf:
  implicit val json: Codec[GoogleOAuthConf] = Codec.from(
    Decoder[GoogleOAuthJson].map(json => json.canonical),
    deriveEncoder[GoogleOAuthConf]
  )
