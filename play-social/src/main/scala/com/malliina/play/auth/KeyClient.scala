package com.malliina.play.auth

import java.time.Instant

import com.malliina.http.{FullUrl, OkClient}
import com.malliina.play.auth.Execution.cached
import play.api.libs.json.Reads

import scala.concurrent.Future

object KeyClient {
  def microsoft(clientIds: Seq[String], http: OkClient): KeyClient =
    MicrosoftCodeValidator.keyClient(clientIds, http)

  def google(clientIds: Seq[String], http: OkClient): KeyClient =
    GoogleCodeValidator.keyClient(clientIds, http)
}

class KeyClient(val knownUrl: FullUrl, validator: TokenValidator, val http: OkClient) {
  def validate(token: TokenValue): Future[Either[AuthError, Verified]] =
    fetchKeys().mapRight { keys =>
      validator.validate(token, keys, Instant.now)
    }

  def fetchKeys(): Future[Either[AuthError, Seq[KeyConf]]] =
    fetchConf().flatMapRight { conf =>
      fetchJson[JWTKeys](conf.jwksUri).mapR(_.keys)
    }

  def fetchConf() = fetchJson[AuthEndpoints](knownUrl)

  def fetchJson[T: Reads](url: FullUrl): Future[Either[AuthError, T]] =
    http.getAs[T](url).map(_.left.map(e => OkError(e)))
}
