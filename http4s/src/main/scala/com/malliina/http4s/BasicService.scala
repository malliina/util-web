package com.malliina.http4s

import cats.Applicative
import cats.data.NonEmptyList
import cats.syntax.all.toFunctorOps
import com.malliina.http.Errors
import com.malliina.http4s.FeedbackSupport
import com.malliina.http4s.BasicService.noCache
import io.circe.syntax.EncoderOps
import org.http4s.CacheDirective.{`must-revalidate`, `no-cache`, `no-store`}
import org.http4s.circe.CirceInstances
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{Location, `Cache-Control`}
import org.http4s.{EntityEncoder, Request, Response, Uri, syntax}

object BasicService:
  val noCacheDirectives = NonEmptyList.of(`no-cache`(), `no-store`, `must-revalidate`)
  val noCache = `Cache-Control`(noCacheDirectives)

trait BasicService[F[_]: Applicative]
  extends syntax.AllSyntax
  with Http4sDsl[F]
  with CirceInstances
  with FeedbackSupport[F]:
  def ok[A](a: A)(using EntityEncoder[F, A]) = Ok(a, noCache)

  def seeOther(uri: Uri): F[Response[F]] =
    SeeOther(Location(uri)).map(_.putHeaders(noCache))

  def notFound(req: Request[F]): F[Response[F]] =
    NotFound(Errors.single(s"Not found: '${req.uri}'.").asJson, noCache)

  def serverError(using Applicative[F]): F[Response[F]] =
    InternalServerError(Errors.single(s"Server error.").asJson, noCache)

  def badRequest[A](a: A)(using EntityEncoder[F, A]): F[Response[F]] =
    BadRequest(a, noCache)
