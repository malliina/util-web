package com.mle.play.controllers

import java.nio.file.{Files, Path, Paths}

import com.mle.concurrent.FutureImplicits.RichFuture
import com.mle.play.auth.{Auth, BasicCredentials}
import com.mle.util.Log
import play.api.libs.iteratee.{Done, Input, Iteratee}
import play.api.libs.{Files => PlayFiles}
import play.api.mvc.Results._
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 *
 * @author mle
 */
class AuthRequest[A](user: String, request: Request[A], val cookie: Option[Cookie] = None)
  extends AuthenticatedRequest[A, String](user, request)

class FileUploadRequest[A](val files: Seq[Path], user: String, request: Request[A]) extends AuthRequest(user, request)

class OneFileUploadRequest[A](val file: Path, user: String, request: Request[A]) extends AuthRequest(user, request)

case class AuthResult(user: String, cookie: Option[Cookie] = None)

trait BaseSecurity extends Log {


  def authenticateFromSession(implicit request: RequestHeader): Option[String] =
    request.session.get(Security.username) //.filter(_.nonEmpty)

  def authenticateFromHeader(implicit request: RequestHeader): Option[String] =
    headerAuth(validateCredentials)

  /**
   * Basic HTTP authentication.
   *
   * The "Authorization" request header should be like: "Basic base64(username:password)", where
   * base64(x) means x base64-encoded.
   *
   * @param f credentials verifier: returns true on success, false otherwise
   * @param request request from which the Authorization header is validated
   * @return the username wrapped in an Option if successfully authenticated, None otherwise
   */
  def headerAuth(f: BasicCredentials => Boolean)(implicit request: RequestHeader): Option[String] = {
    Auth.basicCredentials(request).filter(f).map(_.username)
  }

  /**
   * Authenticates based on the "u" and "p" query string parameters.
   *
   * @param request request
   * @return the username, if successfully authenticated
   */
  def authenticateFromQueryString(implicit request: RequestHeader): Option[String] = {
    Auth.credentialsFromQuery(request).filter(validateCredentials).map(_.username)
  }

  /**
   * Override if you intend to use password authentication.
   *
   * @return True if the credentials are valid; false otherwise. False by default.
   */
  def validateCredentials(creds: BasicCredentials): Boolean = false

  /**
   * Retrieves the authenticated username from the request.
   *
   * Attempts to read the "username" session variable, but if no such thing exists,
   * attempts to authenticate based on the the HTTP Authorization header,
   * finally if that also fails, authenticates based on credentials in the query string.
   *
   * @return the authentication result wrapped in an [[scala.Option]] if successfully authenticated, [[scala.None]] otherwise
   */
  def authenticate(implicit request: RequestHeader): Option[AuthResult] = {
    (authenticateFromSession orElse
      authenticateFromHeader orElse
      authenticateFromQueryString) map lift
  }

  /**
   * Called when an unauthorized request has been made. Also
   * called when a failed authentication attempt is made.
   *
   * Returns HTTP 401 by default; override to handle unauthorized
   * requests in a more app-specific manner.
   *
   * @param req header of request which failed authentication
   * @return "auth failed" result
   */
  protected def onUnauthorized(implicit req: RequestHeader): Result = {
    val ip = req.remoteAddress
    val resource = req.path
    log warn s"Unauthorized request to: $resource from: $ip"
    Unauthorized
  }

  /**
   *
   * @param authFunction
   * @param authAction
   * @tparam U type of user
   * @return
   */
  def LoggedSecureAction[U](authFunction: RequestHeader => Option[U])(authAction: U => EssentialAction): EssentialAction =
    Security.Authenticated(req => authFunction(req), req => onUnauthorized(req))(user => Logged(authAction(user)))

  def AuthActionAsync(f: AuthRequest[AnyContent] => Future[Result]) =
    AuthenticatedLogged(user => Action.async(req => f(new AuthRequest(user.user, req, user.cookie))))

  def LoggedSecureActionAsync[U](authFunction: RequestHeader => Future[U])(authAction: U => EssentialAction) =
    AuthenticatedAsync(authFunction, req => onUnauthorized(req))(user => Logged(authAction(user)))

  def AuthenticatedAsync[A](authFunction: RequestHeader => Future[A],
                            onUnauthorized: RequestHeader => Result)(action: A => EssentialAction): EssentialAction = {
    val f2: RequestHeader => Future[Option[A]] = req => authFunction(req).map(a => Some(a)).recoverAll(_ => None)
    AuthenticatedAsync2(f2, onUnauthorized)(action)
  }

  def AuthenticatedAsync2[A](authFunction: RequestHeader => Future[Option[A]],
                             onUnauthorized: RequestHeader => Result)(action: A => EssentialAction): EssentialAction = {
    EssentialAction(request => {
      val futureIteratee: Future[Iteratee[Array[Byte], Result]] = authFunction(request)
        .map(userOpt => userOpt.map(user => action(user)(request))
        .getOrElse(Done(onUnauthorized(request), Input.Empty)))
      Iteratee flatten futureIteratee
    })
  }

  def Authenticated(f: AuthResult => EssentialAction): EssentialAction =
    Security.Authenticated(req => authenticate(req), unAuthorizedRequest => onUnauthorized(unAuthorizedRequest))(f)

  def Authenticated(f: => EssentialAction): EssentialAction = Authenticated(user => f)

  def AuthenticatedLogged(f: AuthResult => EssentialAction): EssentialAction = Authenticated(user => Logged(user, f))

  def AuthenticatedLogged(f: => EssentialAction): EssentialAction = AuthenticatedLogged(_ => f)

  def AuthAction(f: AuthRequest[AnyContent] => Result) =
    AuthenticatedLogged(user => Action(req => f(new AuthRequest(user.user, req, user.cookie))))

  /**
   * Logs authenticated requests.
   */
  def Logged(user: AuthResult, f: AuthResult => EssentialAction) =
    EssentialAction(request => {
      val qString = request.rawQueryString
      // removes query string from logged line if it contains a password, assumes password is in 'p' parameter
      def queryString =
        if (qString != null && qString.length > 0 && !qString.contains("p=")) s"?$qString"
        else ""
      log info s"User: ${user.user} from: ${request.remoteAddress} requests: ${request.path}$queryString"
      f(user)(request)
    })

  def Logged(action: EssentialAction): EssentialAction = EssentialAction(req => {
    log debug s"Request: ${req.path} from: ${req.remoteAddress}"
    action(req)
  })

  val uploadDir = Paths get sys.props("java.io.tmpdir")

  protected def saveFiles(request: Request[MultipartFormData[PlayFiles.TemporaryFile]]): Seq[Path] =
    request.body.files.map(file => {
      val dest = uploadDir resolve file.filename
      if (!Files.exists(dest))
        file.ref.moveTo(dest.toFile, replace = true)
      dest
    })

  protected def lift(user: String) = AuthResult(user)
}
