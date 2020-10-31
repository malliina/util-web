package com.malliina.play.auth

import java.math.BigInteger
import java.security.SecureRandom

import com.malliina.http.FullUrl
import com.malliina.play.auth.AuthValidator.Callback
import com.malliina.play.auth.CodeValidator.log
import com.malliina.play.auth.OAuthKeys._
import com.malliina.play.http.FullUrls
import com.malliina.play.http.HttpConstants.NoCacheRevalidate
import com.malliina.values.ErrorMessage
import play.api.Logger
import play.api.http.HeaderNames.CACHE_CONTROL
import play.api.libs.json.Reads
import play.api.mvc.Results.Redirect
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

object CodeValidator {
  private val log = Logger(getClass)
  private val rng = new SecureRandom()

  def randomString(): String = new BigInteger(130, rng).toString(32)
}

/**
  * @tparam U type of user object, e.g. Username, Email, AppUser, String
  */
trait CodeValidator[U, V] extends AuthValidator with OAuthValidator[V] {

  /** Returns either a successfully validated user object or an AuthError if validation fails.
    *
    * The returned Future fails with a ResponseException if any network request fails.
    *
    * @param code auth code
    * @param req request
    * @return a user object or a failure
    */
  def validate(
    code: Code,
    redirectUrl: FullUrl,
    requestNonce: Option[String]
  ): Future[Either[AuthError, U]]

  def onOutcome(outcome: Either[AuthError, U], req: RequestHeader): Result

  def validateCallback(req: RequestHeader): Future[Result] =
    validateCallback(
      Callback(
        req.getQueryString(State),
        req.session.get(State),
        req.getQueryString(CodeKey),
        req.session.get(Nonce),
        FullUrls(redirCall, req)
      )
    ).map { e => onOutcome(e, req) }

  def validateCallback(cb: Callback): Future[Either[AuthError, U]] = {
    val isStateOk = cb.requestState.exists(rs => cb.sessionState.contains(rs))
    if (isStateOk) {
      cb.codeQuery
        .map { code => validate(Code(code), cb.redirectUrl, cb.requestNonce) }
        .getOrElse {
          log.error(s"Authentication failed, code missing.")
          fut(Left(OAuthError(ErrorMessage("Code missing."))))
        }
    } else {
      val detailed = (cb.requestState, cb.sessionState) match {
        case (Some(rs), Some(ss)) => s"Got '$rs', expected '$ss'."
        case (Some(rs), None)     => s"Got '$rs', but found nothing to compare to."
        case (None, Some(ss))     => s"No state in request, expected '$ss'."
        case _                    => "No state in request and nothing to compare to either."
      }
      log.error(s"Authentication failed, state mismatch. $detailed")
      fut(Left(OAuthError(ErrorMessage("State mismatch."))))
    }
  }

  /** Adds a random state parameter and an optional nonce and returns a redirect to the authorization URL with all
    * parameters set.
    *
    * @param authorizationEndpoint authorizaton URL
    * @param authParams parameters, unencoded
    * @param nonce optional nonce
    * @return a redirect
    */
  def redirResult(
    authorizationEndpoint: FullUrl,
    authParams: Map[String, String],
    nonce: Option[String] = None
  ): Result = {
    val state = randomString()
    val encodedParams = (authParams ++ Map(State -> state)).map { case (k, v) => k -> urlEncode(v) }
    val url = authorizationEndpoint.append(s"?${stringify(encodedParams)}")
    val sessionParams = Seq(State -> state) ++ nonce.map(n => Seq(Nonce -> n)).getOrElse(Nil)
    log.info(s"Redirecting with state '$state'...")
    Redirect(url.url)
      .withSession(sessionParams: _*)
      .withHeaders(CACHE_CONTROL -> NoCacheRevalidate)
  }

  protected def urlEncode(s: String): String = AuthValidator.urlEncode(s)
  protected def randomString(): String = CodeValidator.randomString()

  protected def commonAuthParams(authScope: String, rh: RequestHeader): Map[String, String] =
    commonAuthParams(authScope, FullUrls(redirCall, rh))

  protected def commonAuthParams(authScope: String, redirectUrl: FullUrl): Map[String, String] =
    Map(
      RedirectUri -> redirectUrl.url,
      ClientIdKey -> clientConf.clientId.value,
      Scope -> authScope
    )

  /** Not encoded.
    */
  protected def validationParams(code: Code, redirectUrl: FullUrl): Map[String, String] = Map(
    ClientIdKey -> clientConf.clientId.value,
    ClientSecretKey -> clientConf.clientSecret.value,
    RedirectUri -> redirectUrl.url,
    CodeKey -> code.code
  )

  def postForm[T: Reads](url: FullUrl, params: Map[String, String]): Future[T] =
    http.postFormAs[T](url, params)

  def postEmpty[T: Reads](
    url: FullUrl,
    headers: Map[String, String],
    params: Map[String, String]
  ): Future[T] =
    http.postFormAs[T](url, params, headers)

  def getJson[T: Reads](url: FullUrl): Future[T] = http.getAs[T](url)
}
