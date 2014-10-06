package com.mle.oauth

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}

/**
 * @author Michael
 */
case class GoogleOAuthConf(issuer: String,
                           authorizationEndpoint: String,
                           tokenEndpoint: String,
                           userInfoEndpoint: String,
                           revocationEndpoint: String,
                           jwksUri: String,
                           responseTypesSupported: Seq[String],
                           subjectTypesSupported: Seq[String],
                           algorithmsSupported: Seq[String],
                           authMethodsSupported: Seq[String])

object GoogleOAuthConf {
  implicit val jsonReader: Reads[GoogleOAuthConf] = (
    (JsPath \ "issuer").read[String] and
      (JsPath \ "authorization_endpoint").read[String] and
      (JsPath \ "token_endpoint").read[String] and
      (JsPath \ "userinfo_endpoint").read[String] and
      (JsPath \ "revocation_endpoint").read[String] and
      (JsPath \ "jwks_uri").read[String] and
      (JsPath \ "response_types_supported").read[Seq[String]] and
      (JsPath \ "subject_types_supported").read[Seq[String]] and
      (JsPath \ "id_token_alg_values_supported").read[Seq[String]] and
      (JsPath \ "token_endpoint_auth_methods_supported").read[Seq[String]]
    )(GoogleOAuthConf.apply _)
}
