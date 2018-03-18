package com.malliina.play.auth

import java.math.BigInteger
import java.security.SecureRandom

import com.malliina.http.{FullUrl, OkClient}
import com.malliina.play.auth.CodeValidator._
import com.malliina.play.http.FullUrls
import com.malliina.play.models.Email
import play.api.Logger
import play.api.libs.json.Reads
import play.api.mvc.{Call, RequestHeader, Result}

import scala.concurrent.Future

object CodeValidator {
  private val log = Logger(getClass)

  val AuthorizationCode = "authorization_code"
  val ClientId = "client_id"
  val ClientSecret = "client_secret"
  val CodeKey = "code"
  val EmailKey = "email"
  val GrantType = "grant_type"
  val IdTokenKey = "id_token"
  val LoginHint = "login_hint"
  val Nonce = "nonce"
  val RedirectUri = "redirect_uri"
  val ResponseType = "response_type"
  val Scope = "scope"
  val State = "state"

  val scope = "openid email"

  def randomState() = new BigInteger(130, new SecureRandom()).toString(32)
}

trait CodeValidator extends AuthValidator {
  def http: OkClient

  def conf: AuthConf

  def redirCall: Call

  def handler: AuthHandler

  def validate(code: Code, req: RequestHeader): Future[Either[AuthError, Email]]

  override def validateCallback(req: RequestHeader): Future[Result] = {
    val requestState = req.getQueryString(State)
    val sessionState = req.session.get(State)
    val isStateOk = requestState.exists(rs => sessionState.contains(rs))
    if (isStateOk) {
      req.getQueryString(CodeKey).map { code =>
        validate(Code(code), req).map(outcome => handler.resultFor(outcome, req))
      }.getOrElse {
        log.error(s"Authentication failed, code mismatch. $req")
        handler.onUnauthorizedFut(OAuthError("Code mismatch."), req)
      }
    } else {
      log.error(s"Authentication failed, state mismatch. $req")
      handler.onUnauthorizedFut(OAuthError("State mismatch."), req)
    }
  }

  protected def urlEncode(s: String) = AuthValidator.urlEncode(s)

  protected def randomState() = CodeValidator.randomState()

  protected def redirUrl(call: Call, rh: RequestHeader) = urlEncode(FullUrls(call, rh).url)

  /** Not encoded.
    */
  protected def validationParams(code: Code, req: RequestHeader) = {
    Map(
      ClientId -> conf.clientId,
      ClientSecret -> conf.clientSecret,
      RedirectUri -> FullUrls(redirCall, req).url,
      CodeKey -> code.code
    )
  }

  def postForm[T: Reads](url: FullUrl, params: Map[String, String]) =
    http.postFormAs[T](url, params).map(_.left.map(e => OkError(e)))

  def postEmpty[T: Reads](url: FullUrl,
                          headers: Map[String, String],
                          params: Map[String, String]) =
    http.postFormAs[T](url, params, headers).map(_.left.map(e => OkError(e)))

  def getJson[T: Reads](url: FullUrl) = http.getJson[T](url).map(_.left.map(e => OkError(e)))
}