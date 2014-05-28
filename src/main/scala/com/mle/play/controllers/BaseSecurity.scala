package com.mle.play.controllers

import play.api.mvc._
import play.api.http.HeaderNames
import org.apache.commons.codec.binary.Base64
import play.api.mvc.Results._
import com.mle.util.Log
import scala.Some
import play.api.mvc.SimpleResult
import play.api.mvc.Security.AuthenticatedRequest
import java.nio.file.{Paths, Files, Path}
import play.api.libs.{Files => PlayFiles}

/**
 *
 * @author mle
 */
class AuthRequest[A](user: String, request: Request[A]) extends AuthenticatedRequest[A, String](user, request)

class FileUploadRequest[A](val files: Seq[Path], user: String, request: Request[A]) extends AuthRequest(user, request)

class OneFileUploadRequest[A](val file: Path, user: String, request: Request[A]) extends AuthRequest(user, request)

trait BaseSecurity extends Log {

  def authenticateFromSession(implicit request: RequestHeader): Option[String] =
    request.session.get(Security.username) //.filter(_.nonEmpty)

  def authenticateFromHeader(implicit request: RequestHeader): Option[String] = headerAuth(validateCredentials)

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
  def headerAuth(f: (String, String) => Boolean)(implicit request: RequestHeader): Option[String] = {
    request.headers.get(HeaderNames.AUTHORIZATION).flatMap(authInfo => {
      authInfo.split(" ") match {
        case Array(authMethod, encodedCredentials) =>
          new String(Base64.decodeBase64(encodedCredentials.getBytes)).split(":", 2) match {
            case Array(user, pass) if f(user, pass) => Some(user)
            case _ => None
          }
        case _ => None
      }
    })
  }

  /**
   * Authenticates based on the "u" and "p" query string parameters.
   *
   * @param request request
   * @return the username, if successfully authenticated
   */
  def authenticateFromQueryString(implicit request: RequestHeader): Option[String] = {
    val qString = request.queryString
    for (
      u <- qString get "u";
      p <- qString get "p";
      user <- u.headOption;
      pass <- p.headOption
      if validateCredentials(user, pass)
    ) yield user
  }

  def validateCredentials(user: String, pass: String): Boolean

  /**
   * Retrieves the authenticated username from the request.
   *
   * Attempts to read the "username" session variable, but if no such thing exists,
   * attempts to authenticate based on the the HTTP Authorization header,
   * finally if that also fails, authenticates based on credentials in the query string.
   *
   * @return the username wrapped in an [[scala.Option]] if successfully authenticated, [[scala.None]] otherwise
   */
  def authenticate(implicit request: RequestHeader): Option[String] = {
    authenticateFromSession orElse
      authenticateFromHeader orElse
      authenticateFromQueryString
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
  protected def onUnauthorized(implicit req: RequestHeader): SimpleResult = {
    val ip = req.remoteAddress
    val resource = req.path
    log warn s"Unauthorized request to: $resource from: $ip"
    Unauthorized
  }

  def Authenticated(f: String => EssentialAction): EssentialAction =
    Security.Authenticated(req => authenticate(req), req => onUnauthorized(req))(f)

  def Authenticated(f: => EssentialAction): EssentialAction =
    Authenticated(user => f)

  def AuthenticatedLogged(f: String => EssentialAction): EssentialAction =
    Authenticated(user => Logged(user, f))

  def AuthenticatedLogged(f: => EssentialAction): EssentialAction =
    AuthenticatedLogged(_ => f)

  def AuthAction(f: AuthRequest[AnyContent] => SimpleResult) =
    AuthenticatedLogged(user => Action(req => f(new AuthRequest(user, req))))

  /**
   * Logs authenticated requests.
   */
  def Logged(user: String, f: String => EssentialAction) =
    EssentialAction(request => {
      val qString = request.rawQueryString
      // removes query string from logged line if it contains a password, assumes password is in 'p' parameter
      def queryString =
        if (qString != null && qString.length > 0 && !qString.contains("p=")) s"?$qString"
        else ""
      log info s"User: $user from: ${request.remoteAddress} requests: ${request.path}$queryString"
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
}
