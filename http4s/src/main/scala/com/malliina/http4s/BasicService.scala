package com.malliina.http4s

import cats.Applicative
import cats.data.NonEmptyList
import cats.effect.Sync
import cats.implicits.toFlatMapOps
import cats.syntax.all.toFunctorOps
import com.malliina.http.{CSRFToken, Errors}
import com.malliina.http4s.BasicService.noCache
import com.malliina.http4s.FeedbackSupport
import io.circe.syntax.EncoderOps
import org.http4s.CacheDirective.*
import org.http4s.circe.CirceInstances
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{Location, `Cache-Control`}
import org.http4s.server.middleware.CSRF
import org.http4s.{EntityEncoder, Request, Response, Uri, syntax}

import scala.concurrent.duration.FiniteDuration

object BasicService:
  val noCacheDirectives = NonEmptyList.of(`no-cache`(), `no-store`, `must-revalidate`)
  val noCache = `Cache-Control`(noCacheDirectives)

  def cached(duration: FiniteDuration) = `Cache-Control`(
    NonEmptyList.of(`max-age`(duration), `public`)
  )

trait BasicService[F[_]: Applicative]
  extends syntax.AllSyntax
  with Http4sDsl[F]
  with CirceInstances
  with FeedbackSupport[F]:
  def ok[A](a: A)(using EntityEncoder[F, A]) = Ok(a, noCache)

  def seeOther(uri: Uri): F[Response[F]] =
    SeeOther(Location(uri)).map(_.putHeaders(noCache))

  def notFound(req: Request[F]): F[Response[F]] =
    notFoundWith(s"Not found: '${req.uri}'.")

  def notFoundWith(message: String): F[Response[F]] =
    NotFound(Errors.single(message).asJson, noCache)

  def serverError(using Applicative[F]): F[Response[F]] =
    serverErrorWith("Server error.")

  def serverErrorWith(message: String)(using Applicative[F]): F[Response[F]] =
    InternalServerError(Errors.single(message).asJson, noCache)

  def badRequest[A](a: A)(using EntityEncoder[F, A]): F[Response[F]] =
    BadRequest(a, noCache)

trait CSRFSupport[F[_]: Sync]:
  this: BasicService[F] =>
  def csrf: CSRF[F, F]

  def csrfOk[A](content: CSRFToken => A)(using EntityEncoder[F, A]) =
    csrf
      .generateToken[F]
      .flatMap: token =>
        ok(content(CSRFUtils.toToken(token))).map: res =>
          csrf.embedInResponseCookie(res, token)
