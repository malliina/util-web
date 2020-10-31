package com.malliina.play.auth

import java.time.Instant

import com.malliina.http.{FullUrl, OkClient}
import com.malliina.values.TokenValue

import scala.concurrent.{ExecutionContext, Future}

object KeyClient {
  def microsoft(clientIds: Seq[ClientId], http: OkClient): KeyClient =
    MicrosoftCodeValidator.keyClient(clientIds, http)

  def google(clientIds: Seq[ClientId], http: OkClient): KeyClient =
    GoogleCodeValidator.keyClient(clientIds, http)
}

class KeyClient(val knownUrl: FullUrl, validator: TokenValidator, val http: OkClient) {
  implicit val ec: ExecutionContext = http.exec

  def validate(
    token: TokenValue,
    now: Instant = Instant.now()
  ): Future[Either[AuthError, Verified]] =
    fetchKeys().map { keys => validator.validate(token, keys, now) }

  def fetchKeys(): Future[Seq[KeyConf]] =
    for {
      conf <- http.getAs[AuthEndpoints](knownUrl)
      keys <- http.getAs[JWTKeys](conf.jwksUri)
    } yield keys.keys
}
