package com.malliina.http4s

import cats.arrow.FunctionK
import cats.data.Kleisli
import cats.effect.{Async, Concurrent, Sync}
import cats.syntax.all.toFunctorOps
import cats.~>
import com.malliina.http4s.BasicService
import com.malliina.http.{CSRFConf, CSRFToken, Errors}
import com.malliina.http4s.CSRFUtils.{CSRFChecker, defaultFailure}
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.circe.CirceInstances
import org.http4s.server.Middleware
import org.http4s.server.middleware.CSRF
import org.http4s.{Request, Response, Status}

object CSRFUtils:
  type CSRFChecker[F[_]] = Middleware[F, Request[F], Response[F], Request[F], Response[F]]

  def generate[F[_]: Sync](generator: CSRF[F, F]): F[CSRFToken] =
    generator.generateToken[F].map(CSRF.unlift).map(CSRFToken.apply)

  def toToken(t: CSRF.CSRFToken): CSRFToken = CSRFToken(CSRF.unlift(t))

  def defaultFailure[F[_]]: Response[F] =
    Response[F](Status.Forbidden)
      .withHeaders(BasicService.noCache)
      .withEntity(Errors.single("CSRF"))

class CSRFUtils(val conf: CSRFConf) extends CirceInstances:
  def default[F[_]: Sync: Concurrent](onFailure: Response[F] = defaultFailure): F[CSRF[F, F]] =
    build[F, F](FunctionK.id[F], onFailure)

  def build[F[_]: Sync, G[_]: Concurrent](
    gf: G ~> F,
    onFailure: Response[G] = defaultFailure[G]
  ): F[CSRF[F, G]] =
    CSRF
      .generateSigningKey[F]()
      .map: key =>
        CSRF[F, G](key, _ => true)
          .withOnFailure(onFailure)
          .withCSRFCheck(
            CSRF.checkCSRFinHeaderAndForm[F, G](conf.tokenName, gf)
          )
          .withCookieName(conf.cookieName)
          .build

  def middleware[F[_]: Async](csrf: CSRF[F, F]): CSRFChecker[F] =
    http =>
      Kleisli: (r: Request[F]) =>
        val nocheck =
          r.headers
            .get(conf.headerName)
            .map(_.head.value)
            .contains(conf.noCheck)
        val response = http(r)
        if nocheck then response
        else if r.method.isSafe then response
        else csrf.checkCSRF(r, response)
