package com.malliina.play.auth

import com.malliina.play.auth.BasicAuthHandler.log
import com.malliina.play.http.HttpConstants.NoCacheRevalidate
import com.malliina.values.Email
import play.api.Logger
import play.api.http.HeaderNames.CACHE_CONTROL
import play.api.libs.json.Json
import play.api.mvc.Results.{Redirect, Unauthorized}
import play.api.mvc.{Call, Cookie, RequestHeader, Result}

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, DurationInt}

trait AuthHandler extends AuthResults[Email]

/**
  * @tparam U type of user
  */
trait AuthResults[U] {
  def onAuthenticated(user: U, req: RequestHeader): Result

  def onUnauthorized(error: AuthError, req: RequestHeader): Result

  def resultFor(outcome: Either[AuthError, U], req: RequestHeader): Result =
    outcome.fold(
      err => onUnauthorized(err, req),
      user => onAuthenticated(user, req)
    )

  def onUnauthorizedFut(error: AuthError, req: RequestHeader): Future[Result] =
    Future.successful(onUnauthorized(error, req))

  def filter(p: U => Boolean): AuthResults[U] =
    flatMap(user => if (p(user)) Right(user) else Left(PermissionError(s"Unauthorized: '$user'.")))

  def flatMap(f: U => Either[AuthError, U]): AuthResults[U] = {
    val parent = this
    new AuthResults[U] {
      override def onAuthenticated(user: U, req: RequestHeader): Result =
        f(user).fold(e => parent.onUnauthorized(e, req), user => parent.onAuthenticated(user, req))

      override def onUnauthorized(error: AuthError, req: RequestHeader): Result =
        parent.onUnauthorized(error, req)
    }
  }
}

object BasicAuthHandler {
  private val log = Logger(getClass)

  val DefaultMaxAge: Duration = 3650.days
  val DefaultSessionKey = "username"
  val LastIdCookie = "lastId"

  def apply(successCall: Call,
            lastIdKey: String = LastIdCookie,
            authorize: Email => Either[AuthError, Email] = email => Right(email),
            sessionKey: String = DefaultSessionKey,
            lastIdMaxAge: Option[Duration] = Option(DefaultMaxAge)): BasicAuthHandler =
    new BasicAuthHandler(successCall, lastIdKey, authorize, sessionKey, lastIdMaxAge)
}

class BasicAuthHandler(val successCall: Call,
                       val lastIdKey: String,
                       authorize: Email => Either[AuthError, Email],
                       val sessionKey: String,
                       val lastIdMaxAge: Option[Duration]) extends AuthHandler {
  override def onAuthenticated(email: Email, req: RequestHeader): Result =
    authorize(email).fold(
      err => onUnauthorized(err, req),
      email => {
        log.info(s"Logging in '$email' through OAuth code flow.")
        Redirect(successCall)
          .withSession(sessionKey -> email.email)
          .withCookies(Cookie(lastIdKey, email.email, lastIdMaxAge.map(_.toSeconds.toInt)))
          .withHeaders(CACHE_CONTROL -> NoCacheRevalidate)
      })

  override def onUnauthorized(error: AuthError, req: RequestHeader): Result = {
    log.error(s"${error.message} $req")
    Unauthorized(Json.obj("message" -> "Authentication failed."))
      .withNewSession
      .withHeaders(CACHE_CONTROL -> NoCacheRevalidate)
  }
}
