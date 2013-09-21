package com.mle.play.controllers

import play.api.mvc.{Security, RequestHeader}
import play.api.http.HeaderNames
import scala.Predef.String
import org.apache.commons.codec.binary.Base64
import play.api.mvc.Results._
import scala.Some
import play.api.mvc.SimpleResult
import play.api.mvc.Security.AuthenticatedBuilder
import com.mle.util.Log

/**
 *
 * @author mle
 */
trait BaseSecurity extends Log {

  def authenticateFromSession(implicit request: RequestHeader): Option[String] =
    request.session.get(Security.username)//.filter(_.nonEmpty)

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

  protected def onAuthFail(req: RequestHeader): SimpleResult = {
    val ip = req.remoteAddress
    val path = req.path
    log warn s"Unauthorized request to: $path from: $ip"
    Unauthorized
  }

  class AuthAction[U](f: RequestHeader => Option[U])
    extends AuthenticatedBuilder[U](f, onAuthFail)

  object BasicAuthAction extends AuthAction(req => authenticate(req))

}
