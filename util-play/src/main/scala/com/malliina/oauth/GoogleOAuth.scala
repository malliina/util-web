package com.malliina.oauth

import akka.stream.Materializer
import com.malliina.oauth.GoogleOAuth._
import org.apache.commons.codec.binary.Base64
import play.api.Logger
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json._
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.Future

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
}

/**
  * @see https://developers.google.com/accounts/docs/OAuth2Login
  */
class GoogleOAuth(clientId: String, clientSecret: String, mat: Materializer) extends AutoCloseable {
  implicit val ec = mat.executionContext

  implicit val client = AhcWSClient()(mat)

  def discover(): Future[GoogleOAuthConf] = jsonRequest[GoogleOAuthConf](GoogleOAuth.DiscoverUri)

  def tokenRequest(tokenEndpoint: String,
                   code: String,
                   redirectUri: String): Future[TokenResponse] = {
    def stringify(pairs: (String, String)*) = pairs.map(p => p._1 + "=" + p._2).mkString("&")

    val query = stringify(
      Code -> code,
      ClientId -> clientId,
      ClientSecret -> clientSecret,
      RedirectUri -> redirectUri,
      GrantType -> AuthorizationCode
    )
    client.url(tokenEndpoint)
      .withHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.FORM)
      .post(query)
      .map(response => response.json.as[TokenResponse])
  }

  def resolveEmail(tokenEndpoint: String, code: String, redirectUri: String): Future[String] =
    tokenRequest(tokenEndpoint, code, redirectUri) map email

  private def jsonRequest[T: Reads](url: String) =
    client.url(url).get().map(response => response.json.as[T])

  def authRequestUri(authEndpoint: String, redirectUri: String, state: String) = {
    s"$authEndpoint?$ClientId=$clientId&$ResponseType=$Code&$Scope=openid%20email&$RedirectUri=$redirectUri&$State=$state&$LoginHint=sub"
  }

  def email(tokenResponse: TokenResponse): String = {
    log debug s"Decoding: ${tokenResponse.id_token}"
    // the id_token string contains three base64url-encoded json values separated by dots
    val encArr = tokenResponse.id_token.split('.')
    val decoder = new Base64(true)
    val Array(headers, claims, signature) = encArr.map(enc => new String(decoder.decode(enc)))
    Json.parse(claims).as[IdToken].email
  }

  def close() = client.close()
}
