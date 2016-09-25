package com.malliina.play.controllers

import akka.stream.Materializer
import com.malliina.play.auth.{Auth, BasicCredentials}
import com.malliina.play.controllers.BaseSecurity.log
import com.malliina.play.http.{AuthedRequest, CookiedRequest, FullRequest}
import com.malliina.play.models.Username
import play.api.Logger
import play.api.libs.streams.Accumulator
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.Future

class BaseSecurity(sessionUserKey: String, val mat: Materializer) {
  def this(mat: Materializer) = this(Security.username, mat)

  implicit val ec = mat.executionContext

  def authenticateFromSession(request: RequestHeader): Future[Option[Username]] =
    fut(request.session.get(sessionUserKey).map(Username.apply))

  /** Basic HTTP authentication.
    *
    * The "Authorization" request header should be like: "Basic base64(username:password)", where
    * base64(x) means x base64-encoded.
    *
    * @param request request from which the Authorization header is validated
    * @return the username wrapped in an Option if successfully authenticated, None otherwise
    */
  def authenticateFromHeader(request: RequestHeader): Future[Option[Username]] =
    performAuthentication(Auth.basicCredentials(request))

  /** Authenticates based on the "u" and "p" query string parameters.
    *
    * @param request request
    * @return the username, if successfully authenticated
    */
  def authenticateFromQueryString(request: RequestHeader): Future[Option[Username]] =
    performAuthentication(Auth.credentialsFromQuery(request))

  def performAuthentication(creds: Option[BasicCredentials]) =
    creds.map(validateUser).getOrElse(fut(None))

  def validateUser(creds: BasicCredentials): Future[Option[Username]] =
    validateCredentials(creds).map(isValid => if (isValid) Option(creds.username) else None)

  /** Override if you intend to use password authentication.
    *
    * @return True if the credentials are valid; false otherwise. False by default.
    */
  def validateCredentials(creds: BasicCredentials): Future[Boolean] = fut(false)

  /** Retrieves the authenticated username from the request.
    *
    * Attempts to read the "username" session variable, but if no such thing exists,
    * attempts to authenticate based on the the HTTP Authorization header,
    * finally if that also fails, authenticates based on credentials in the query string.
    *
    * @return the authentication result wrapped in an [[scala.Option]] if successfully authenticated, [[scala.None]] otherwise
    */
  def authenticate(request: RequestHeader): Future[Option[AuthedRequest]] = {
    import com.malliina.play.concurrent.FutureOps2
    authenticateFromSession(request)
      .checkOrElse(_.nonEmpty, authenticateFromHeader(request))
      .checkOrElse(_.nonEmpty, authenticateFromQueryString(request))
      .map(_.map(user => lift(user, request)))
  }

  def checkOrElse[T, U >: T](f: Future[T], orElse: => Future[U], check: T => Boolean): Future[U] =
    f.flatMap(t => if (check(t)) fut(t) else orElse)

  /** Called when an unauthorized request has been made. Also
    * called when a failed authentication attempt is made.
    *
    * Returns HTTP 401 by default; override to handle unauthorized
    * requests in a more app-specific manner.
    *
    * @param request header of request which failed authentication
    * @return "auth failed" result
    */
  protected def onUnauthorized(request: RequestHeader): Result = {
    val ip = request.remoteAddress
    val resource = request.path
    log warn s"Unauthorized request to: $resource from: $ip"
    Unauthorized
  }

  def loggedSecureAction[U](authFunction: RequestHeader => Option[U])(authAction: U => EssentialAction): EssentialAction =
    Security.Authenticated(req => authFunction(req), req => onUnauthorized(req))(user => logged(authAction(user)))

  def authActionAsync(f: CookiedRequest[AnyContent, AuthedRequest] => Future[Result]) =
    authenticatedLogged(user => Action.async(req => f(new CookiedRequest(user, req, user.cookie))))

  def loggedSecureActionAsync[U](authFunction: RequestHeader => Future[U])(authAction: U => EssentialAction) =
    authenticatedAsync2(authFunction, req => onUnauthorized(req))(user => logged(authAction(user)))

  def authAction(f: FullRequest => Result) =
    authenticatedLogged(user => Action(req => f(user.fillAny(req))))

  def authenticatedLogged(f: AuthedRequest => EssentialAction): EssentialAction =
    authenticated(user => logged(user, f))

  def authenticatedLogged(f: => EssentialAction): EssentialAction =
    authenticatedLogged(_ => f)

  def authenticated(f: => EssentialAction): EssentialAction =
    authenticated(user => f)

  def authenticated(f: AuthedRequest => EssentialAction): EssentialAction =
    authenticatedAsync(req => authenticate(req), unAuthorizedRequest => onUnauthorized(unAuthorizedRequest))(f)

  /** Logs authenticated requests.
    */
  def logged(user: AuthedRequest, f: AuthedRequest => EssentialAction) =
    EssentialAction { request =>
      val qString = request.rawQueryString
      // removes query string from logged line if it contains a password, assumes password is in 'p' parameter
      def queryString =
        if (qString != null && qString.length > 0 && !qString.contains("p=")) s"?$qString"
        else ""
      log info s"User: ${user.user} from: ${request.remoteAddress} requests: ${request.path}$queryString"
      f(user)(request)
    }

  def logged(action: EssentialAction): EssentialAction = EssentialAction { req =>
    log debug s"Request: ${req.path} from: ${req.remoteAddress}"
    action(req)
  }

  /**
    * @param authFunction authentication that fails the Future if authentication fails
    * @return an authenticated action
    */
  def authenticatedAsync2[A](authFunction: RequestHeader => Future[A],
                             onUnauthorized: RequestHeader => Result)(action: A => EssentialAction): EssentialAction = {
    import com.malliina.concurrent.FutureOps
    val f2: RequestHeader => Future[Option[A]] = req => authFunction(req).map(a => Some(a)).recoverAll(_ => None)
    authenticatedAsync(f2, onUnauthorized)(action)
  }

  /** Async version of Security.Authenticated.
    *
    * @param auth           auth function
    * @param onUnauthorized callback if auth fails
    * @param action         authenticated action
    * @tparam A type of user+request
    * @return an authenticated action
    */
  def authenticatedAsync[A](auth: RequestHeader => Future[Option[A]],
                            onUnauthorized: RequestHeader => Result)(action: A => EssentialAction): EssentialAction =
    EssentialAction { request =>
      val futureAccumulator = auth(request) map { maybeUser =>
        maybeUser
          .map(user => action(user).apply(request))
          .getOrElse(Accumulator.done(onUnauthorized(request)))
      }
      Accumulator.flatten(futureAccumulator)(mat)
    }

  protected def lift(user: Username, request: RequestHeader) = AuthedRequest(user, request)

  private def fut[T](t: T): Future[T] = Future.successful(t)
}

object BaseSecurity {
  private val log = Logger(getClass)
}
