package com.mle.oauth

import java.io.Closeable

import com.mle.util.Log
import com.ning.http.client.AsyncHttpClientConfig
import org.apache.commons.codec.binary.Base64
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * @see https://developers.google.com/accounts/docs/OAuth2Login
 * @author Michael
 */
class GoogleOAuth(clientId: String, clientSecret: String) extends Closeable with Log {

  import com.mle.oauth.GoogleOAuth._

  implicit val client = new NingWSClient(new AsyncHttpClientConfig.Builder().build())

  def discover(): Future[GoogleOAuthConf] = jsonRequest[GoogleOAuthConf](GoogleOAuth.discoverUri)

  def tokenRequest(tokenEndpoint: String,
                   code: String,
                   redirectUri: String): Future[TokenResponse] = {
    def stringify(pairs: (String, String)*) = pairs.map(p => p._1 + "=" + p._2).mkString("&")

    val query = stringify(
      CODE -> code,
      CLIENT_ID -> clientId,
      CLIENT_SECRET -> clientSecret,
      REDIRECT_URI -> redirectUri,
      GRANT_TYPE -> AUTHORIZATION_CODE
    )
    WS.clientUrl(tokenEndpoint)
      .withHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.FORM)
      .post(query)
      .map(response => response.json.as[TokenResponse])
  }

  def resolveEmail(tokenEndpoint: String, code: String, redirectUri: String): Future[String] =
    tokenRequest(tokenEndpoint, code, redirectUri) map email

  def close() = client.close()

  private def jsonRequest[T](url: String)(implicit fjs: Reads[T]) = WS.clientUrl(url).get().map(response => response.json.as[T])

  def authRequestUri(authEndpoint: String, redirectUri: String, state: String) = {
    s"$authEndpoint?$CLIENT_ID=$clientId&$RESPONSE_TYPE=$CODE&$SCOPE=openid%20email&$REDIRECT_URI=$redirectUri&$STATE=$state&$LOGIN_HINT=sub"
  }

  def email(tokenResponse: TokenResponse): String = {
    log debug s"Decoding: ${tokenResponse.id_token}"
    // the id_token string contains three base64url-encoded json values separated by dots
    val encArr = tokenResponse.id_token.split('.')
    val decoder = new Base64(true)
    val Array(headers, claims, signature) = encArr.map(enc => new String(decoder.decode(enc)))
    Json.parse(claims).as[IdToken].email
  }

}

object GoogleOAuth {
  val discoverUri = "https://accounts.google.com/.well-known/openid-configuration"
  val emailScope = "https://www.googleapis.com/auth/userinfo.email"
  val CLIENT_ID = "client_id"
  val CLIENT_SECRET = "client_secret"
  val REDIRECT_URI = "redirect_uri"
  val GRANT_TYPE = "grant_type"
  val RESPONSE_TYPE = "response_type"
  val LOGIN_HINT = "login_hint"
  val AUTHORIZATION_CODE = "authorization_code"
  val CODE = "code"
  val STATE = "state"
  val SCOPE = "scope"
}

