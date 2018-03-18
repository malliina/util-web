package com.malliina.play.auth

import com.malliina.play.auth.BasicAuthHandler.log
import com.malliina.play.models.Email
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Results.{Redirect, Unauthorized}
import play.api.mvc.{Call, RequestHeader, Result}

import scala.concurrent.Future

trait AuthHandler {
  def onAuthenticated(email: Email, req: RequestHeader): Result

  def onUnauthorized(error: AuthError, req: RequestHeader): Result

  def resultFor(outcome: Either[AuthError, Email], req: RequestHeader): Result = {
    outcome.fold(
      err => onUnauthorized(err, req),
      email => onAuthenticated(email, req)
    )
  }

  def onUnauthorizedFut(error: AuthError, req: RequestHeader): Future[Result] =
    Future.successful(onUnauthorized(error, req))

  def filter(p: Email => Boolean): AuthHandler =
    flatMap(email => if (p(email)) Right(email) else Left(PermissionError(s"Unauthorized: '$email'.")))

  def flatMap(f: Email => Either[AuthError, Email]): AuthHandler = {
    val parent = this
    new AuthHandler {
      override def onAuthenticated(email: Email, req: RequestHeader): Result =
        f(email).fold(e => parent.onUnauthorized(e, req), email => parent.onAuthenticated(email, req))

      override def onUnauthorized(error: AuthError, req: RequestHeader): Result =
        parent.onUnauthorized(error, req)
    }
  }
}

object BasicAuthHandler {
  private val log = Logger(getClass)

  def apply(successCall: Call): BasicAuthHandler = new BasicAuthHandler(successCall)
}

class BasicAuthHandler(val successCall: Call,
                       authorize: Email => Either[AuthError, Email] = email => Right(email),
                       val sessionKey: String = "username") extends AuthHandler {
  override def onAuthenticated(email: Email, req: RequestHeader): Result = {
    authorize(email).fold(err => onUnauthorized(err, req), email => {
      log.info(s"Logging in '$email' through OAuth code flow.")
      Redirect(successCall).withSession(sessionKey -> email.email)
    })
  }

  override def onUnauthorized(error: AuthError, req: RequestHeader): Result = {
    log.error(s"${error.message} for $req")
    Unauthorized(Json.obj("message" -> "Authentication failed."))
  }
}
