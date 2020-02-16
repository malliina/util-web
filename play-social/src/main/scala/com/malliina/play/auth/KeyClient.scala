package com.malliina.play.auth

import java.time.Instant

import com.malliina.http.{FullUrl, OkClient}
import com.malliina.play.auth.Execution.cached
import com.malliina.values.TokenValue

import scala.concurrent.Future

object KeyClient {
  def microsoft(clientIds: Seq[String], http: OkClient): KeyClient =
    MicrosoftCodeValidator.keyClient(clientIds, http)

  def google(clientIds: Seq[String], http: OkClient): KeyClient =
    GoogleCodeValidator.keyClient(clientIds, http)
}

class KeyClient(val knownUrl: FullUrl, validator: TokenValidator, val http: OkClient) {
  def validate(token: TokenValue): Future[Either[AuthError, Verified]] =
    fetchKeys().map { keys =>
      validator.validate(token, keys, Instant.now)
    }

  def fetchKeys(): Future[Seq[KeyConf]] =
    for {
      conf <- http.getAs[AuthEndpoints](knownUrl)
      keys <- http.getAs[JWTKeys](conf.jwksUri)
    } yield keys.keys
}
