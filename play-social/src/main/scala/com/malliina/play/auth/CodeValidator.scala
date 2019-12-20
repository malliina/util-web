package com.malliina.play.auth

import java.math.BigInteger
import java.security.SecureRandom

import com.malliina.http.FullUrl
import com.malliina.play.auth.CodeValidator.log
import com.malliina.play.auth.OAuthKeys.{
  ClientId => ClientIdKey,
  ClientSecret => ClientSecretKey,
  _
}
import com.malliina.play.http.FullUrls
import com.malliina.play.http.HttpConstants.NoCacheRevalidate
import com.malliina.values.ErrorMessage
import play.api.Logger
import play.api.http.HeaderNames.CACHE_CONTROL
import play.api.libs.json.Reads
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, RequestHeader, Result}

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
  def validate(code: Code, req: RequestHeader): Future[Either[AuthError, U]]

  def onOutcome(outcome: Either[AuthError, U], req: RequestHeader): Result

  override def validateCallback(req: RequestHeader): Future[Result] = {
    val requestState = req.getQueryString(State)
    val sessionState = req.session.get(State)
    val isStateOk = requestState.exists(rs => sessionState.contains(rs))
    if (isStateOk) {
      req
        .getQueryString(CodeKey)
        .map { code =>
          validate(Code(code), req).map { outcome =>
            onOutcome(outcome, req)
          }
        }
        .getOrElse {
          log.error(s"Authentication failed, code missing. $req")
          fut(onOutcome(Left(OAuthError(ErrorMessage("Code missing."))), req))
        }
    } else {
      val detailed = (requestState, sessionState) match {
        case (Some(rs), Some(ss)) => s"Got '$rs', expected '$ss'."
        case (Some(rs), None)     => s"Got '$rs', but found nothing to compare to."
        case (None, Some(ss))     => s"No state in request, expected '$ss'."
        case _                    => "No state in request and nothing to compare to either."
      }
      log.error(s"Authentication failed, state mismatch. $detailed $req")
      fut(onOutcome(Left(OAuthError(ErrorMessage("State mismatch."))), req))
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
  protected def redirUrl(call: Call, rh: RequestHeader): String = urlEncode(FullUrls(call, rh).url)

  protected def commonAuthParams(authScope: String, rh: RequestHeader): Map[String, String] = Map(
    RedirectUri -> FullUrls(redirCall, rh).url,
    ClientIdKey -> clientConf.clientId,
    Scope -> authScope
  )

  /** Not encoded.
    */
  protected def validationParams(code: Code, req: RequestHeader): Map[String, String] = Map(
    ClientIdKey -> clientConf.clientId,
    ClientSecretKey -> clientConf.clientSecret,
    RedirectUri -> FullUrls(redirCall, req).url,
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
