package com.malliina.play.auth

import com.malliina.http.{FullUrl, OkClient}
import com.malliina.play.auth.CodeValidator.EmailKey
import com.malliina.values.Email

object GoogleCodeValidator {
  val knownUrlGoogle =
    FullUrl("https", "accounts.google.com", "/.well-known/openid-configuration")

  val EmailVerified = "email_verified"

  def apply(oauth: OAuthConf[Email]): GoogleCodeValidator = apply(google(oauth))

  def apply(conf: CodeValidationConf[Email]): GoogleCodeValidator =
    new GoogleCodeValidator(conf)

  def google(oauth: OAuthConf[Email]): CodeValidationConf[Email] = CodeValidationConf(
    oauth,
    AuthCodeConf(
      "Google",
      oauth.redirCall,
      oauth.conf,
      keyClient(oauth.conf.clientId, oauth.http),
      Map.empty
    )
  )

  def keyClient(clientId: String, http: OkClient): KeyClient =
    new KeyClient(knownUrlGoogle, GoogleValidator(clientId), http)
}

class GoogleCodeValidator(conf: CodeValidationConf[Email])
  extends StandardOAuth[Email](conf) {

  override def parse(validated: Verified): Either[AuthError, Email] = {
    val emailVerified = validated.readBoolean(GoogleCodeValidator.EmailVerified)
    for {
      _ <- emailVerified.filterOrElse(_ == true, InvalidClaims(validated.token, "Email not verified."))
      email <- validated.readString(EmailKey).map(Email.apply)
    } yield email
  }
}
