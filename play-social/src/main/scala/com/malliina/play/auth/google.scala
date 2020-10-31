package com.malliina.play.auth

import java.time.Instant

import com.malliina.http.{FullUrl, OkClient}
import com.malliina.play.auth.OAuthKeys.EmailKey
import com.malliina.values.{Email, ErrorMessage}

object GoogleValidator {
  val issuers = Seq("https://accounts.google.com", "accounts.google.com").map(Issuer.apply)

  def apply(clientIds: Seq[ClientId]): GoogleValidator = new GoogleValidator(clientIds, issuers)
}

class GoogleValidator(clientIds: Seq[ClientId], issuers: Seq[Issuer])
  extends TokenValidator(issuers) {
  override protected def validateClaims(
    parsed: ParsedJWT,
    now: Instant
  ): Either[JWTError, ParsedJWT] =
    checkContains(Aud, clientIds.map(_.value), parsed).map { _ => parsed }
}

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

  def keyClient(clientIds: Seq[ClientId], http: OkClient): KeyClient =
    new KeyClient(knownUrlGoogle, GoogleValidator(clientIds), http)
}

class GoogleCodeValidator(conf: CodeValidationConf[Email]) extends StandardOAuth[Email](conf) {

  override def parse(validated: Verified): Either[JWTError, Email] = {
    val emailVerified = validated.readBoolean(GoogleCodeValidator.EmailVerified)
    for {
      _ <- emailVerified.filterOrElse(
        _ == true,
        InvalidClaims(validated.token, ErrorMessage("Email not verified."))
      )
      email <- validated.readString(EmailKey).map(Email.apply)
    } yield email
  }
}
