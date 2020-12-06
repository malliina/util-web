package com.malliina.web

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64

import com.malliina.http.{FullUrl, OkClient}
import com.malliina.values.{AccessToken, TokenValue}
import com.malliina.web.TwitterAuthFlow._
import com.malliina.web.Utils.{randomString, urlEncode}
import com.malliina.web.WebHeaders.{Authorization, ContentType}
import okhttp3.Request
import org.apache.commons.codec.digest.{HmacAlgorithms, HmacUtils}

import scala.collection.SortedMap
import scala.concurrent.{ExecutionContext, Future}

object TwitterAuthFlow {
  val OauthTokenKey = "oauth_token"
  val OauthVerifierKey = "oauth_verifier"

  def sign(key: String, in: String) = {
    val digest = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, key).hmac(in)
    new String(Base64.getEncoder.encode(digest), StandardCharsets.UTF_8)
  }

  def signingKey(clientSecret: ClientSecret, tokenSecret: Option[String]) = {
    val clientPart = percentEncode(clientSecret.value)
    val tokenPart = tokenSecret.fold("")(percentEncode)
    s"$clientPart&$tokenPart"
  }

  def percentEncode(in: String) = {
    val encoded = urlEncode(in)
    val strb = new StringBuilder
    var skip = -1
    encoded.zipWithIndex.foreach {
      case (c, i) =>
        if (i != skip) {
          if (c == '*') {
            strb.append("%2A")
          } else if (c == '+') {
            strb.append("%20")
          } else if (c == '%' && i + 1 < encoded.length && (encoded.charAt(i + 1) == '7') && (encoded
                       .charAt(i + 2) == 'E')) {
            strb += '~'
            skip = i + 1
          } else {
            strb.append(c)
          }
        }
    }
    strb.toString()
  }
}

class TwitterAuthFlow(conf: AuthConf, val http: OkClient) extends FlowStart[Future] {
  val brandName = "Twitter"
  val baseUrl = FullUrl.https("api.twitter.com", "")
  val requestTokenUrl = baseUrl / "oauth" / "request_token"
  val accessTokenUrl = baseUrl / "oauth" / "access_token"
  val userInfoUrl = baseUrl / "1.1" / "account" / "verify_credentials.json"
  implicit val ec: ExecutionContext = http.exec

  def authTokenUrl(token: AccessToken) =
    FullUrl("https", "api.twitter.com", s"/oauth/authenticate?oauth_token=$token")

  // TODO this doesn't work, reimplement locally
  def start(redirectUrl: FullUrl, extraParams: Map[String, String]): Future[Start] =
    Future.successful(Start(redirectUrl, extraParams, None))

  def requestToken(redirectUrl: FullUrl): Future[Either[OAuthError, AccessToken]] =
    fetchRequestToken(redirectUrl).map { optTokens =>
      optTokens
        .filter(_.oauthCallbackConfirmed)
        .map { tokens => tokens.oauthToken }
        .toRight(OAuthError("Callback not confirmed."))
    }

  def validateTwitterCallback(
    oauthToken: AccessToken,
    requestToken: AccessToken,
    oauthVerifier: String
  ): Future[Either[OAuthError, TwitterUser]] =
    if (oauthToken == requestToken) {
      fetchAccessToken(oauthToken, oauthVerifier).flatMap { optAccess =>
        optAccess
          .map { access => fetchUser(access).map(Right.apply) }
          .getOrElse(Future.successful(Left(OAuthError("No access token in response."))))
      }
    } else {
      Future.successful(Left(OAuthError(s"Invalid callback parameters.")))
    }

  private def fetchRequestToken(redirUrl: FullUrl): Future[Option[TwitterTokens]] = {
    val encodable = Encodable(buildNonce, Map("oauth_callback" -> redirUrl.url))
    val authHeaderValue = encodable.signed("POST", requestTokenUrl, None)
    http
      .postForm(requestTokenUrl, form = Map.empty, headers = Map(Authorization -> authHeaderValue))
      .map { r => TwitterTokens.fromString(r.asString) }
  }

  private def fetchAccessToken(
    requestToken: AccessToken,
    verifier: String
  ): Future[Option[TwitterAccess]] = {
    val encodable = paramsStringWith(requestToken, buildNonce)
    val authHeaderValue = encodable.signed("POST", accessTokenUrl, None)
    http
      .postForm(
        accessTokenUrl,
        form = Map(OauthVerifierKey -> verifier),
        headers = Map(Authorization -> authHeaderValue, ContentType -> HttpConstants.FormUrlEncoded)
      )
      .map { res => TwitterAccess.fromString(res.asString) }
  }

  private def fetchUser(access: TwitterAccess): Future[TwitterUser] = {
    val queryParams = Map(
      "skip_status" -> "true",
      "include_entities" -> "false",
      "include_email" -> "true"
    )
    val encodable = paramsStringWith(access.oauthToken, buildNonce, queryParams)
    val authHeaderValue = encodable.signed("GET", userInfoUrl, Option(access.oauthTokenSecret))
    val queryString = queryParams.map { case (k, v) => s"$k=$v" }.mkString("&")
    val reqUrl = userInfoUrl.append(s"?$queryString")

    val req =
      new Request.Builder().url(reqUrl.url).addHeader(Authorization, authHeaderValue).get.build()
    http.execute(req).flatMap { res =>
      res
        .parse[TwitterUser]
        .fold(
          err => Future.failed(com.malliina.http.JsonError(err, res, reqUrl).toException),
          user => Future.successful(user)
        )
    }
  }

  private def buildNonce =
    new String(
      Base64.getEncoder.encode(randomString().getBytes(StandardCharsets.UTF_8)),
      StandardCharsets.UTF_8
    )

  private def paramsStringWith(
    token: TokenValue,
    nonce: String,
    map: Map[String, String] = Map.empty
  ) =
    Encodable(nonce, Map(OauthTokenKey -> token.value) ++ map)

  case class Encodable(nonce: String, map: Map[String, String]) {
    private val params = map ++ Map(
      "oauth_consumer_key" -> conf.clientId.value,
      "oauth_nonce" -> nonce,
      "oauth_signature_method" -> "HMAC-SHA1",
      "oauth_timestamp" -> s"${Instant.now().getEpochSecond}",
      "oauth_version" -> "1.0"
    )
    private val encoded = params.map { case (k, v) => (percentEncode(k), percentEncode(v)) }
    val encodedParams = SortedMap(encoded.toSeq: _*)
    val paramsString = percentEncode(encodedParams.map { case (k, v) => s"$k=$v" }.mkString("&"))

    def signed(method: String, url: FullUrl, oauthTokenSecret: Option[String]): String = {
      val signatureBaseString = s"$method&${percentEncode(url.url)}&$paramsString"
      val key = signingKey(conf.clientSecret, oauthTokenSecret)
      val signature = sign(key, signatureBaseString)
      val headerParams = encodedParams ++ Map("oauth_signature" -> percentEncode(signature))
      val authHeaderValues = headerParams.map { case (k, v) => s"""$k="$v"""" }.mkString(", ")
      s"OAuth $authHeaderValues"
    }
  }
}
