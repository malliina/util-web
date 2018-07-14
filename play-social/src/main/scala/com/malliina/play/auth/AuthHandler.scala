package com.malliina.play.auth

import com.malliina.play.auth.BasicAuthHandler.log
import com.malliina.values.Email
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Results.{Redirect, Unauthorized}
import play.api.mvc.{Call, Cookie, RequestHeader, Result}

import scala.concurrent.Future

trait AuthHandler extends AuthHandlerBase[Email]

/**
  *
  * @tparam U type of user
  */
trait AuthHandlerBase[U] {
  def onAuthenticated(user: U, req: RequestHeader): Result

  def onUnauthorized(error: AuthError, req: RequestHeader): Result

  def resultFor(outcome: Either[AuthError, U], req: RequestHeader): Result = {
    outcome.fold(
      err => onUnauthorized(err, req),
      user => onAuthenticated(user, req)
    )
  }

  def onUnauthorizedFut(error: AuthError, req: RequestHeader): Future[Result] =
    Future.successful(onUnauthorized(error, req))

  def filter(p: U => Boolean): AuthHandlerBase[U] =
    flatMap(user => if (p(user)) Right(user) else Left(PermissionError(s"Unauthorized: '$user'.")))

  def flatMap(f: U => Either[AuthError, U]): AuthHandlerBase[U] = {
    val parent = this
    new AuthHandlerBase[U] {
      override def onAuthenticated(user: U, req: RequestHeader): Result =
        f(user).fold(e => parent.onUnauthorized(e, req), user => parent.onAuthenticated(user, req))

      override def onUnauthorized(error: AuthError, req: RequestHeader): Result =
        parent.onUnauthorized(error, req)
    }
  }
}

object BasicAuthHandler {
  private val log = Logger(getClass)

  val LastIdCookie = "last_id"

  def apply(successCall: Call, lastIdKey: String = LastIdCookie): BasicAuthHandler =
    new BasicAuthHandler(successCall, lastIdKey)
}

class BasicAuthHandler(val successCall: Call,
                       val lastIdKey: String,
                       authorize: Email => Either[AuthError, Email] = email => Right(email),
                       val sessionKey: String = "username") extends AuthHandler {
  override def onAuthenticated(email: Email, req: RequestHeader): Result = {
    authorize(email).fold(err => onUnauthorized(err, req), email => {
      log.info(s"Logging in '$email' through OAuth code flow.")
      Redirect(successCall)
        .withSession(sessionKey -> email.email)
        .withCookies(Cookie(lastIdKey, email.email))
    })
  }

  override def onUnauthorized(error: AuthError, req: RequestHeader): Result = {
    log.error(s"${error.message} for $req")
    Unauthorized(Json.obj("message" -> "Authentication failed."))
  }
}
