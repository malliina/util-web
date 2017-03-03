package com.malliina.play.controllers

import akka.stream.Materializer
import com.malliina.play.auth._
import com.malliina.play.controllers.BaseSecurity.log
import com.malliina.play.http.{AuthedRequest, CookiedRequest, FullRequest, Proxies}
import com.malliina.play.models.Username
import play.api.Logger
import play.api.libs.streams.Accumulator
import play.api.mvc._

import scala.concurrent.Future

class BaseSecurity(val mat: Materializer, auth: AuthBundle[Username]) {
  implicit val ec = mat.executionContext
  val authenticator = auth.authenticator.transform { (req, user) =>
    Right(AuthedRequest(user, req))
  }

  /** Override if you intend to use password authentication.
    *
    * @return True if the credentials are valid; false otherwise. False by default.
    */
  def validateCredentials(creds: BasicCredentials): Future[Boolean] = fut(false)

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
  def authenticate(rh: RequestHeader): Future[Either[AuthFailure, AuthedRequest]] =
    authenticator.authenticate(rh)

  def checkOrElse[T, U >: T](f: Future[T], orElse: => Future[U], check: T => Boolean): Future[U] =
    f.flatMap(t => if (check(t)) fut(t) else orElse)

  def authActionAsync(f: CookiedRequest[AnyContent, AuthedRequest] => Future[Result]) =
    authenticatedLogged(user => Action.async(req => f(new CookiedRequest(user, req, user.cookie))))

  def authAction(f: FullRequest => Result) =
    authenticatedLogged(user => Action(req => f(user.fillAny(req))))

  def authenticatedLogged(f: AuthedRequest => EssentialAction): EssentialAction =
    authenticated(user => logged(user, f))

  def authenticatedLogged(f: => EssentialAction): EssentialAction =
    authenticatedLogged(_ => f)

  def authenticated(f: => EssentialAction): EssentialAction =
    authenticated(_ => f)

  def authenticated(f: AuthedRequest => EssentialAction): EssentialAction =
    authenticatedAsync(req => authenticate(req), failure => onUnauthorized(failure))(f)

  /** Logs authenticated requests.
    */
  def logged(user: AuthedRequest, f: AuthedRequest => EssentialAction) =
    EssentialAction { rh =>
      val qString = rh.rawQueryString

      // removes query string from logged line if it contains a password, assumes password is in 'p' parameter
      def queryString =
        if (qString != null && qString.length > 0 && !qString.contains("p=")) s"?$qString"
        else ""

      log info s"User '${user.user}' from '${Proxies.realAddress(rh)}' requests '${rh.path}$queryString'."
      f(user)(rh)
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
    * @tparam A type of user+request
    * @return an authenticated action
    */
  def authenticatedAsync[A](auth: RequestHeader => Future[Either[AuthFailure, A]],
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

  protected def lift(user: Username, request: RequestHeader) = AuthedRequest(user, request)

  private def fut[T](t: T): Future[T] = Future.successful(t)
}

object BaseSecurity {
  private val log = Logger(getClass)
}
