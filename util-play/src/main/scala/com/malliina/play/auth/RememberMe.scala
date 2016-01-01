package com.malliina.play.auth

import com.malliina.play.controllers.AuthResult
import com.malliina.util.Log
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Cookie, CookieBaker, DiscardingCookie, RequestHeader}

import scala.concurrent.Future
import scala.util.Random

/**
 * Adapted from https://github.com/wsargent/play20-rememberme
 *
 * @author Michael
 */
object RememberMe extends CookieBaker[UnAuthToken] with Log {
  val COOKIE_NAME = "REMEMBER_ME"
  val SERIES_NAME = "series"
  val USER_ID_NAME = "userId"
  val TOKEN_NAME = "token"
  val discardingCookie = DiscardingCookie(COOKIE_NAME)

  import scala.concurrent.duration.DurationInt

  override def maxAge: Option[Int] = Some(365.days.toSeconds.toInt)

  /**
   * @param req request
   * @return the browser's possibly stored token
   */
  def readToken(req: RequestHeader): Option[UnAuthToken] = {
    val cookie = req.cookies get COOKIE_NAME
    log debug s"Reading cookie: $cookie"
    val tokenMaybeEmpty = decodeFromCookie(cookie)
    if (!tokenMaybeEmpty.isEmpty) {
      log debug s"Read token: $tokenMaybeEmpty"
    }
    Option(tokenMaybeEmpty).filterNot(_.isEmpty)
  }

  override val emptyCookie: UnAuthToken = UnAuthToken.empty

  override protected def serialize(cookie: UnAuthToken): Map[String, String] = Map(
    USER_ID_NAME -> cookie.user,
    SERIES_NAME -> cookie.series.toString,
    TOKEN_NAME -> cookie.token.toString
  )

  /**
   * The API says we must return a token, even if deserialization fails, so we introduce the concept of an "empty" token
   * and filter it away in `readToken(RequestHeader)`.
   *
   * @param data token data
   * @return a token
   */
  override protected def deserialize(data: Map[String, String]): UnAuthToken = try {
    val maybeToken =
      for {
        u <- data get USER_ID_NAME
        s <- data get SERIES_NAME
        t <- data get TOKEN_NAME
      } yield UnAuthToken(u, s.toLong, t.toLong)
    maybeToken getOrElse UnAuthToken.empty
  } catch {
    case nfe: NumberFormatException => UnAuthToken.empty
  }
}

class RememberMe(store: TokenStore) extends Log {
  /**
   * @return the authenticated user, along with an optional cookie to include
   */
  def authenticateFromCookie(req: RequestHeader): Future[Option[AuthResult]] = {
    authenticateToken(req) map (maybeToken => maybeToken.map(token => AuthResult(token.user, Some(cookify(token)))))
  }

  /**
   * @return an authenticated token
   */
  def authenticateToken(req: RequestHeader): Future[Option[Token]] = {
    authenticate(req).map(_.right.toOption)
  }

  def authenticate(req: RequestHeader): Future[Either[AuthFailure, Token]] ={
    RememberMe.readToken(req).map(cookieAuth) getOrElse {
      log debug s"Found no token in request: ${req.cookies}"
      Future.successful(Left(CookieMissing))
    }
  }

  def cookify(token: Token) = RememberMe.encodeAsCookie(token.asUnAuth)

  def persistNewCookie(loggedInUser: String): Future[Cookie] = createToken(loggedInUser).map(cookify)

  private def createToken(loggedInUser: String): Future[Token] = {
    val token = Token(loggedInUser, Random.nextLong(), Random.nextLong())
    (store persist token).map(_ => token)
  }

  private def cookieAuth(attempt: UnAuthToken): Future[Either[AuthFailure, Token]] = {
    log debug s"Authenticating: $attempt"
    val user = attempt.user
    store.findToken(user, attempt.series).flatMap(maybeToken => {
      maybeToken.map(savedToken => {
        if (savedToken.token == attempt.token) {
          /**
           * I believe the intention is to ensure that a browser cannot reuse another browser's token.
           *
           * The token is replaced with a new one at each successful token authentication, while the series remains the
           * same; this updated cookie is then sent to the browser. The series acts as a browser identifier. So, if
           * there's a token mismatch, it suggests some other actor has authenticated using this browser's token, which is
           * suspect.
           */
          log info s"Cookie authentication succeeded. Updating token."
          for {
            _ <- store remove savedToken
            newToken = Token(user, attempt.series, Random.nextLong())
            _ <- store persist newToken
          } yield Right(newToken)
        } else {
          log warn s"The saved token did not match the one from the request. Refusing access."
          (store removeAll user).map(_ => Left(InvalidCookie))
        }
      }).getOrElse {
        log debug s"Unable to authenticate token: $attempt"
        Future.successful(Left(InvalidCredentials))
      }
    })
  }
}
