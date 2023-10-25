package com.malliina.web

import cats.effect.Sync
import com.malliina.http.{FullUrl, HttpClient}
import com.malliina.values.ErrorMessage
import com.malliina.web.MicrosoftValidator.knownUrlMicrosoft
import com.malliina.web.OAuthKeys.{Scope, scope}

import java.time.Instant

object MicrosoftAuthFlow:
  def apply[F[_]: Sync](creds: AuthConf, http: HttpClient[F]): EmailAuthFlow[F] =
    EmailAuthFlow(conf(creds, http))

  def conf[F[_]: Sync](creds: AuthConf, http: HttpClient[F]): AuthCodeConf[F] = AuthCodeConf(
    "Microsoft",
    creds,
    keyClient(Seq(creds.clientId), http),
    extraStartParams = Map("response_mode" -> "query"),
    extraValidateParams = Map(Scope -> scope)
  )

  def keyClient[F[_]: Sync](clientIds: Seq[ClientId], http: HttpClient[F]): KeyClient[F] =
    KeyClient(knownUrlMicrosoft, MicrosoftValidator(clientIds), http)

object MicrosoftValidator:
  // https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-v2-tokens
  val knownUrlMicrosoft =
    FullUrl("https", "login.microsoftonline.com", "/common/v2.0/.well-known/openid-configuration")
  val issuerMicrosoftConsumer =
    Issuer("https://login.microsoftonline.com/9188040d-6c67-4c5b-b112-36a304b66dad/v2.0")

class MicrosoftValidator(clientIds: Seq[ClientId], issuer: Issuer = issuerMicrosoftConsumer)
  extends TokenValidator(issuer):
  override protected def validateClaims(
    parsed: ParsedJWT,
    now: Instant
  ): Either[JWTError, ParsedJWT] =
    for
      _ <- checkContains(Aud, clientIds.map(_.value), parsed)
      _ <- checkNbf(parsed, now)
    yield parsed

  def checkNbf(parsed: ParsedJWT, now: Instant): Either[JWTError, Instant] =
    StaticTokenValidator
      .read(parsed.token, parsed.claims.getNotBeforeTime, ErrorMessage(NotBefore))
      .flatMap: nbf =>
        val nbfInstant = nbf.toInstant
        if now.isBefore(nbfInstant) then Left(NotYetValid(parsed.token, nbfInstant, now))
        else Right(nbfInstant)
