package com.malliina.web

import cats.effect.Sync
import com.malliina.http.{FullUrl, HttpClient}
import com.malliina.values.{Email, ErrorMessage}
import com.malliina.web.OAuthKeys.{EmailKey, EmailVerified}

object GoogleAuthFlow:
  val knownUrlGoogle =
    FullUrl("https", "accounts.google.com", "/.well-known/openid-configuration")

  def apply[F[_]: Sync](conf: AuthCodeConf[F]): GoogleAuthFlow[F] = new GoogleAuthFlow(conf)
  def apply[F[_]: Sync](creds: AuthConf, http: HttpClient[F]): GoogleAuthFlow[F] =
    apply(conf(creds, http))

  def conf[F[_]: Sync](creds: AuthConf, http: HttpClient[F]) = AuthCodeConf(
    "Google",
    creds,
    keyClient(Seq(creds.clientId), http),
    Map.empty
  )

  def keyClient[F[_]: Sync](clientIds: Seq[ClientId], http: HttpClient[F]): KeyClient[F] =
    KeyClient(knownUrlGoogle, GoogleValidator(clientIds), http)

class GoogleAuthFlow[F[_]: Sync](conf: AuthCodeConf[F])
  extends DiscoveringAuthFlow[F, Email](conf)
  with LoginHint[F]:
  override def parse(validated: Verified): Either[JWTError, Email] =
    val emailVerified = validated.readBoolean(EmailVerified)
    for
      _ <- emailVerified.filterOrElse(
        _ == true,
        InvalidClaims(validated.token, ErrorMessage("Email not verified."))
      )
      email <- validated.readString(EmailKey).map(Email.apply)
    yield email
