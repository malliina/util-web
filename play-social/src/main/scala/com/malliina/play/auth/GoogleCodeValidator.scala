package com.malliina.play.auth

import com.malliina.http.{FullUrl, OkClient}
import com.malliina.play.auth.CodeValidator.EmailKey
import com.malliina.values.Email

object GoogleCodeValidator {
  val knownUrlGoogle =
    FullUrl("https", "accounts.google.com", "/.well-known/openid-configuration")

  val EmailVerified = "email_verified"

  def apply(oauth: OAuthConf[Email]): EmailValidator =
    EmailValidator(google(oauth))

  def apply(code: CodeValidationConf[Email]): DiscoveringCodeValidator[Email] =
    EmailValidator.map(code) { validated =>
      val emailVerified = validated.readBoolean(GoogleCodeValidator.EmailVerified)
      for {
        _ <- emailVerified.filterOrElse(_ == true, InvalidClaims(validated.token, "Email not verified."))
        email <- validated.readString(EmailKey).map(Email.apply)
      } yield email
    }

  def google(oauth: OAuthConf[Email]) = CodeValidationConf(
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
