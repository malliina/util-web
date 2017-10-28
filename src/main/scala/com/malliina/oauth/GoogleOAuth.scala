package com.malliina.oauth

import com.malliina.concurrent.ExecutionContexts
import com.malliina.http.{AsyncHttp, FullUrl}
import com.malliina.oauth.GoogleOAuth._
import com.malliina.play.models.Email
import org.apache.commons.codec.binary.Base64
import play.api.Logger
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

object GoogleOAuth {
  private val log = Logger(getClass)

  val DiscoverUri = "https://accounts.google.com/.well-known/openid-configuration"
  val EmailScope = "https://www.googleapis.com/auth/userinfo.email"
  val ClientId = "client_id"
  val ClientSecret = "client_secret"
  val RedirectUri = "redirect_uri"
  val GrantType = "grant_type"
  val ResponseType = "response_type"
  val LoginHint = "login_hint"
  val AuthorizationCode = "authorization_code"
  val Code = "code"
  val State = "state"
  val Scope = "scope"

  def apply(creds: GoogleOAuthKey): GoogleOAuth =
    new GoogleOAuth(creds)(ExecutionContexts.cached)
}

/**
  * @see https://developers.google.com/accounts/docs/OAuth2Login
  */
class GoogleOAuth(creds: GoogleOAuthKey)(implicit val ec: ExecutionContext)
  extends GoogleOAuthLike {
  def this(clientId: String, clientSecret: String) =
    this(GoogleOAuthCreds(clientId, clientSecret))(ExecutionContexts.cached)

  val clientId = creds.clientId
  val httpClient = new AsyncHttp()

  def discover(): Future[GoogleOAuthConf] = jsonRequest[GoogleOAuthConf](GoogleOAuth.DiscoverUri)

  def tokenRequest(tokenEndpoint: FullUrl,
                   code: String,
                   redirectUri: FullUrl): Future[TokenResponse] = {
    val params = Map(
      Code -> code,
      ClientId -> clientId,
      ClientSecret -> creds.clientSecret,
      RedirectUri -> redirectUri.url,
      GrantType -> AuthorizationCode
    )
    httpClient.postForm(tokenEndpoint.url, params).map(_.parse[TokenResponse].get)
  }

  def resolveEmail(tokenEndpoint: FullUrl, code: String, redirectUri: FullUrl): Future[Email] =
    tokenRequest(tokenEndpoint, code, redirectUri) map email

  private def jsonRequest[T: Reads](url: String): Future[T] = {
    httpClient.get(url).map(_.parse[T].get)
  }

  def authRequestUri(authEndpoint: FullUrl, redirectUri: FullUrl, state: String): FullUrl =
    authEndpoint.append(s"?$ClientId=$clientId&$ResponseType=$Code&$Scope=openid%20email&$RedirectUri=$redirectUri&$State=$state&$LoginHint=sub")

  def email(tokenResponse: TokenResponse): Email = {
    log debug s"Decoding: ${tokenResponse.id_token}"
    // the id_token string contains three base64url-encoded json values separated by dots
    val encArr = tokenResponse.id_token.split('.')
    val decoder = new Base64(true)
    val Array(headers, claims, signature) = encArr.map(enc => new String(decoder.decode(enc)))
    Json.parse(claims).as[IdToken].email
  }

  def close(): Unit = httpClient.close()
}
