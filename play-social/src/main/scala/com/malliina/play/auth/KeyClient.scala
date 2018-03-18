package com.malliina.play.auth

import java.time.Instant

import com.malliina.http.{FullUrl, OkClient}
import com.malliina.play.auth.Execution.cached
import play.api.libs.json.Reads

import scala.concurrent.Future

object KeyClient {
  // https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-v2-tokens
  val knownUrlMicrosoft =
    FullUrl("https", "login.microsoftonline.com", "/common/v2.0/.well-known/openid-configuration")

  val knownUrlGoogle =
    FullUrl("https", "accounts.google.com", "/.well-known/openid-configuration")

  def microsoft(clientId: String, http: OkClient): KeyClient =
    new KeyClient(knownUrlMicrosoft, MicrosoftValidator(clientId), http)

  def google(clientId: String, http: OkClient): KeyClient =
    new KeyClient(knownUrlGoogle, GoogleValidator(clientId), http)
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
    http.getJson[T](url).map(_.left.map(e => OkError(e)))
}
