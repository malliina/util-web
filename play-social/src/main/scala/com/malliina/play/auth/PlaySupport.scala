package com.malliina.play.auth

import com.malliina.http.FullUrl
import com.malliina.play.auth.DiscoveringCodeValidator.log
import com.malliina.play.auth.PlaySupport.log
import com.malliina.play.http.FullUrls
import com.malliina.util.AppLogger
import com.malliina.web.HttpConstants.NoCacheRevalidate
import com.malliina.web.OAuthKeys.{CodeKey, Nonce, State}
import com.malliina.web.Utils.{randomString, stringify, urlEncode}
import com.malliina.web.WebHeaders.CacheControl
import com.malliina.web.{AuthError, Callback, CallbackValidator, FlowStart}
import play.api.libs.json.Json
import play.api.mvc.Results.{BadGateway, Redirect}
import play.api.mvc.{Call, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

object PlaySupport {
  private val log = AppLogger(getClass)
}

trait PlaySupport[U] { self: CallbackValidator[U] =>
  import PlaySupport.log

  def redirCall: Call

  def onOutcome(outcome: Either[AuthError, U], req: RequestHeader): Result

  def validateCallback(req: RequestHeader)(implicit ec: ExecutionContext): Future[Result] =
    self
      .validateCallback(
        Callback(
          req.getQueryString(State),
          req.session.get(State),
          req.getQueryString(CodeKey),
          req.session.get(Nonce),
          FullUrls(redirCall, req)
        )
      )
      .map { e => onOutcome(e, req) }

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
      .withHeaders(CacheControl -> NoCacheRevalidate)
  }
}

object PlayFlow {
  private val log = AppLogger(getClass)
}

trait PlayFlow[U] extends PlaySupport[U] with FlowStart[Future] { self: CallbackValidator[U] =>

  /** The initial result that initiates sign-in.
    */
  def start(
    req: RequestHeader,
    extraParams: Map[String, String] = Map.empty
  )(implicit ec: ExecutionContext): Future[Result] =
    start(FullUrls(redirCall, req), extraParams)
      .map { s => redirResult(s.authorizationEndpoint, s.params, s.nonce) }
      .recover {
        case e =>
          PlayFlow.log.error(s"HTTP error.", e)
          BadGateway(Json.obj("message" -> "HTTP error."))
      }
}
