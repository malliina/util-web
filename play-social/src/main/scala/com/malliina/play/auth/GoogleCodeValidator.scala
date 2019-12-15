package com.malliina.play.auth

import com.malliina.http.{FullUrl, OkClient}
import com.malliina.play.auth.OAuthKeys.EmailKey
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
      keyClient(Seq(oauth.conf.clientId), oauth.http),
      Map.empty
    )
  )

  def keyClient(clientIds: Seq[String], http: OkClient): KeyClient =
    new KeyClient(knownUrlGoogle, GoogleValidator(clientIds), http)
}

class GoogleCodeValidator(conf: CodeValidationConf[Email]) extends StandardOAuth[Email](conf) {

  override def parse(validated: Verified): Either[JWTError, Email] = {
    val emailVerified = validated.readBoolean(GoogleCodeValidator.EmailVerified)
    for {
      _ <- emailVerified.filterOrElse(
        _ == true,
        InvalidClaims(validated.token, "Email not verified.")
      )
      email <- validated.readString(EmailKey).map(Email.apply)
    } yield email
  }
}
