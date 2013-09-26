package com.mle.play.controllers

import play.api.mvc._
import play.api.http.HeaderNames
import scala.Predef.String
import org.apache.commons.codec.binary.Base64
import play.api.mvc.Results._
import com.mle.util.Log
import scala.Some
import play.api.mvc.SimpleResult
import com.mle.play.actions.Actions.MappingActionBuilder
import play.api.mvc.Security.AuthenticatedRequest
import java.nio.file.{Paths, Files, Path}
import play.api.libs.{Files => PlayFiles}
import com.mle.util.Implicits._
import play.api.mvc.BodyParsers.parse

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
    val path = req.path
    log warn s"Unauthorized request to: $path from: $ip"
    Unauthorized
  }

  trait AuthFailureHandling[R[C]] extends MappingActionBuilder[R] {
    override protected def onFailure[A](request: Request[A]): SimpleResult =
      onUnauthorized(request)
  }

  /**
   * Ensures that the user is authenticated before serving the request.
   *
   * Note that this is silly, because now the request body is parsed
   * regardless of whether authentication succeeded or not. That's
   * especially unacceptable if the web service handles file uploads.
   *
   * It is therefore better to use <code>SecureAction</code> which
   * performs authentication before body parsing.
   */
  class AuthActionBuilder extends MappingActionBuilder[AuthRequest] with AuthFailureHandling[AuthRequest] {
    override def map[A](request: Request[A]): Either[SimpleResult, AuthRequest[A]] =
      authenticate(request)
        .map(user => Right(new AuthRequest[A](user, request)))
        .getOrElse(Left(onFailure(request)))
  }

  object AuthAction extends AuthActionBuilder

  def SecureAction[U](auth: RequestHeader => Option[U])(f: U => EssentialAction) =
    Security.Authenticated(req => auth(req), req => onUnauthorized(req))(f)

  def UserAction(f: String => EssentialAction) = SecureAction[String](req => authenticate(req))(f)

  val uploadDir = Paths get sys.props("java.io.tmpdir")

  /**
   * An action for multipart/form-data uploaded files. Assumes file uploads require authentication.
   * Saves the uploaded file in a temporary directory.
   *
   * @param builder action builder
   * @param f builds a result from a request with a file
   * @return the action
   */
  def UploadAction(builder: AuthActionBuilder = AuthAction)(f: FileUploadRequest[MultipartFormData[PlayFiles.TemporaryFile]] => SimpleResult): Action[MultipartFormData[PlayFiles.TemporaryFile]] =
    builder(parse.multipartFormData)(req => {
      val files = saveFiles(req)
      f(new FileUploadRequest(files, req.user, req))
    })

  def HeadUploadAction(builder: AuthActionBuilder = AuthAction)(f: OneFileUploadRequest[MultipartFormData[PlayFiles.TemporaryFile]] => SimpleResult) =
    UploadAction(builder)(req =>
      req.files.headOption
        .map(firstFile => f(new OneFileUploadRequest(firstFile, req.user, req)))
        .getOrElse(BadRequest)
    )

  private def saveFiles(request: Request[MultipartFormData[PlayFiles.TemporaryFile]]): Seq[Path] =
    request.body.files.map(file => {
      val dest = uploadDir / file.filename
      if (!Files.exists(dest))
        file.ref.moveTo(dest.toFile, replace = true)
      dest
    })
}
