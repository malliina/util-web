package com.malliina.play.controllers

import akka.stream.Materializer
import com.malliina.play.auth._
import com.malliina.play.controllers.BaseSecurity.log
import com.malliina.play.http._
import play.api.Logger
import play.api.libs.streams.Accumulator
import play.api.mvc._

import scala.concurrent.Future

object BaseSecurity {
  private val log = Logger(getClass)
}

/**
  *
  * @param mat  materilalizer
  * @param auth authenticator
  * @tparam A type of authenticated user
  */
class BaseSecurity[A](auth: AuthBundle[A], val mat: Materializer) {
  implicit val ec = mat.executionContext

  /** Called when an unauthorized request has been made. Also
    * called when a failed authentication attempt is made.
    *
    * @param failure header auth failure, including request headers
    * @return "auth failed" result
    */
  protected def onUnauthorized(failure: AuthFailure): Result =
    auth.onUnauthorized(failure)

  /** Retrieves the authenticated username from the request.
    *
    * Attempts to read the "username" session variable, but if no such thing exists,
    * attempts to authenticate based on the the HTTP Authorization header,
    * finally if that also fails, authenticates based on credentials in the query string.
    *
    * @return the authentication result
    */
  def authenticate(rh: RequestHeader): Future[Either[AuthFailure, A]] =
    auth.authenticator.authenticate(rh)

  def authActionAsync(f: A => Future[Result]) =
    authenticatedLogged(user => Action.async(_ => f(user)))

  def authAction(f: A => Result) =
    authenticatedLogged(user => Action(_ => f(user)))

  def authenticatedLogged(f: A => EssentialAction): EssentialAction =
    authenticated(user => logged(user, f))

  def authenticatedLogged(f: => EssentialAction): EssentialAction =
    authenticatedLogged(_ => f)

  def authenticated(f: => EssentialAction): EssentialAction =
    authenticated(_ => f)

  def authenticated(f: A => EssentialAction): EssentialAction =
    authenticatedAsync(req => authenticate(req), failure => onUnauthorized(failure))(f)

  /** Logs authenticated requests.
    */
  def logged(user: A, f: A => EssentialAction) =
    EssentialAction { rh =>
      logAuth(user, rh)
      f(user)(rh)
    }

  def logAuth(user: A, rh: RequestHeader) = {
    val qString = rh.rawQueryString

    // removes query string from logged line if it contains a password, assumes password is in 'p' parameter
    def queryString =
      if (qString != null && qString.length > 0 && !qString.contains("p=")) s"?$qString"
      else ""

    log info s"Authenticated user from '${Proxies.realAddress(rh)}' requests '${rh.path}$queryString'."
  }

  def logged(action: EssentialAction): EssentialAction = EssentialAction { rh =>
    log debug s"Request '${rh.path}' from '${Proxies.realAddress(rh)}'."
    action(rh)
  }

  /** Async version of Security.Authenticated.
    *
    * @param auth           auth function
    * @param onUnauthorized callback if auth fails
    * @param action         authenticated action
    * @return an authenticated action
    */
  def authenticatedAsync(auth: RequestHeader => Future[Either[AuthFailure, A]],
                         onUnauthorized: AuthFailure => Result)(action: A => EssentialAction): EssentialAction =
    EssentialAction { rh =>
      val futureAccumulator = auth(rh) map { authResult =>
        authResult.fold(
          failure => Accumulator.done(onUnauthorized(failure)),
          success => action(success).apply(rh)
        )
      }
      Accumulator.flatten(futureAccumulator)(mat)
    }
}
