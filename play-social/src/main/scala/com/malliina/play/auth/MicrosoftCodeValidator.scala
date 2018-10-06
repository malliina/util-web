package com.malliina.play.auth

import com.malliina.http.{FullUrl, OkClient}
import com.malliina.play.auth.CodeValidator.Scope
import com.malliina.values.Email

object MicrosoftCodeValidator {
  // https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-v2-tokens
  val knownUrlMicrosoft =
    FullUrl("https", "login.microsoftonline.com", "/common/v2.0/.well-known/openid-configuration")

  def apply[U](oauth: OAuthConf[Email]) = EmailValidator(microsoft(oauth))

  def microsoft[U](oauth: OAuthConf[U]) = CodeValidationConf(
    oauth,
    AuthCodeConf(
      "Microsoft",
      oauth.redirCall,
      oauth.conf,
      keyClient(oauth.conf.clientId, oauth.http),
      extraStartParams = Map("response_mode" -> "query"),
      extraValidateParams = Map(Scope -> CodeValidator.scope)
    )
  )

  def keyClient(clientId: String, http: OkClient): KeyClient =
    new KeyClient(knownUrlMicrosoft, MicrosoftValidator(clientId), http)
}
