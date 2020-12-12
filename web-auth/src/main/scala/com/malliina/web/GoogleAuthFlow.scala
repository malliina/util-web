package com.malliina.web

import com.malliina.http.{FullUrl, OkClient}
import com.malliina.values.{Email, ErrorMessage}
import com.malliina.web.GoogleAuthFlow.EmailVerified
import com.malliina.web.OAuthKeys.EmailKey

import scala.concurrent.Future

object GoogleAuthFlow {
  val knownUrlGoogle =
    FullUrl("https", "accounts.google.com", "/.well-known/openid-configuration")
  val EmailVerified = "email_verified"

  def apply(conf: AuthCodeConf): GoogleAuthFlow = new GoogleAuthFlow(conf)
  def apply(creds: AuthConf, http: OkClient): GoogleAuthFlow = apply(conf(creds, http))

  def conf(creds: AuthConf, http: OkClient) = AuthCodeConf(
    "Google",
    creds,
    keyClient(Seq(creds.clientId), http),
    Map.empty
  )

  def keyClient(clientIds: Seq[ClientId], http: OkClient): KeyClient =
    new KeyClient(knownUrlGoogle, GoogleValidator(clientIds), http)
}

class GoogleAuthFlow(conf: AuthCodeConf)
  extends DiscoveringAuthFlow[Email](conf)
  with LoginHint[Future] {
  override def parse(validated: Verified): Either[JWTError, Email] = {
    val emailVerified = validated.readBoolean(EmailVerified)
    for {
      _ <- emailVerified.filterOrElse(
        _ == true,
        InvalidClaims(validated.token, ErrorMessage("Email not verified."))
      )
      email <- validated.readString(EmailKey).map(Email.apply)
    } yield email
  }
}
