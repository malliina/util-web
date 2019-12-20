package com.malliina.play.auth

import java.time.Instant

import com.malliina.http.{FullUrl, OkClient}
import com.malliina.play.auth.OAuthKeys.{Scope, scope}
import com.malliina.play.auth.StaticTokenValidator.read
import com.malliina.values.{Email, ErrorMessage}

object MicrosoftValidator {
  val issuerMicrosoftConsumer =
    "https://login.microsoftonline.com/9188040d-6c67-4c5b-b112-36a304b66dad/v2.0"

  def apply(clientIds: Seq[String]): MicrosoftValidator =
    new MicrosoftValidator(clientIds, issuerMicrosoftConsumer)
}

class MicrosoftValidator(clientIds: Seq[String], issuer: String) extends TokenValidator(issuer) {
  override protected def validateClaims(
    parsed: ParsedJWT,
    now: Instant
  ): Either[JWTError, ParsedJWT] =
    for {
      _ <- checkContains(Aud, clientIds, parsed)
      _ <- checkNbf(parsed, now)
    } yield parsed

  def checkNbf(parsed: ParsedJWT, now: Instant): Either[JWTError, Instant] =
    read(parsed.token, parsed.claims.getNotBeforeTime, ErrorMessage(NotBefore)).flatMap { nbf =>
      val nbfInstant = nbf.toInstant
      if (now.isBefore(nbfInstant)) Left(NotYetValid(parsed.token, nbfInstant, now))
      else Right(nbfInstant)
    }
}

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
      keyClient(Seq(oauth.conf.clientId), oauth.http),
      extraStartParams = Map("response_mode" -> "query"),
      extraValidateParams = Map(Scope -> scope)
    )
  )

  def keyClient(clientIds: Seq[String], http: OkClient): KeyClient =
    new KeyClient(knownUrlMicrosoft, MicrosoftValidator(clientIds), http)
}
