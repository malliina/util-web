package com.malliina.web

import java.time.Instant
import cats.effect.Sync
import cats.syntax.all.*
import com.malliina.http.{FullUrl, HttpClient}
import com.malliina.values.TokenValue

class KeyClient[F[_]: Sync](
  val knownUrl: FullUrl,
  validator: TokenValidator,
  val http: HttpClient[F]
):
  def validate(token: TokenValue, now: Instant = Instant.now()): F[Either[AuthError, Verified]] =
    fetchKeys().map { keys => validator.validate(token, keys, now) }

  def fetchKeys(): F[Seq[KeyConf]] =
    for
      conf <- http.getAs[AuthEndpoints](knownUrl)
      keys <- http.getAs[JWTKeys](conf.jwksUri)
    yield keys.keys
