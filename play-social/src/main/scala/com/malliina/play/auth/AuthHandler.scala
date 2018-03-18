package com.malliina.play.auth

import com.malliina.play.auth.BasicAuthHandler.log
import com.malliina.play.models.Email
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Results.{Redirect, Unauthorized}
import play.api.mvc.{Call, RequestHeader, Result}

import scala.concurrent.Future

trait AuthHandler {
  def resultFor(outcome: Either[AuthError, Email], req: RequestHeader): Result

  def unauthorizedFut(req: RequestHeader): Future[Result]

  def unauthorized(req: RequestHeader): Result
}

object BasicAuthHandler {
  private val log = Logger(getClass)

  def apply(successCall: Call): BasicAuthHandler = new BasicAuthHandler(successCall)
}

class BasicAuthHandler(successCall: Call, sessionKey: String = "username") extends AuthHandler {
  def resultFor(outcome: Either[AuthError, Email], req: RequestHeader): Result = {
    outcome.fold(
      err => {
        log.error(s"${err.message} for $req")
        unauthorized(req)
      },
      email => {
        log.info(s"Logging in '$email' through OAuth code flow.")
        Redirect(successCall).withSession(sessionKey -> email.email)
      }
    )
  }

  def unauthorizedFut(req: RequestHeader): Future[Result] =
    fut(unauthorized(req))

  def unauthorized(req: RequestHeader): Result =
    Unauthorized(Json.obj("message" -> "Authentication failed."))

  protected def fut[T](t: T) = Future.successful(t)
}
