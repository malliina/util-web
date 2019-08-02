package com.malliina.play.auth

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64

import com.malliina.http.{FullUrl, ResponseException}
import com.malliina.play.auth.TwitterValidator._
import com.malliina.play.http.FullUrls
import com.malliina.values.Email
import okhttp3.Request
import org.apache.commons.codec.digest.{HmacAlgorithms, HmacUtils}
import play.api.http.HeaderNames.{AUTHORIZATION, CONTENT_TYPE}
import play.api.http.MimeTypes
import play.api.mvc.Results.Redirect
import play.api.mvc.{RequestHeader, Result}

import scala.collection.SortedMap
import scala.concurrent.Future

object TwitterValidator {
  val OauthTokenKey = "oauth_token"
  val OauthVerifierKey = "oauth_verifier"

  def apply(oauth: OAuthConf[Email]): TwitterValidator = new TwitterValidator(oauth)

  def sign(key: String, in: String) = {
    val digest = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, key).hmac(in)
    new String(Base64.getEncoder.encode(digest), StandardCharsets.UTF_8)
  }

  def signingKey(clientSecret: String, tokenSecret: Option[String]) = {
    val clientPart = percentEncode(clientSecret)
    val tokenPart = tokenSecret.fold("")(percentEncode)
    s"$clientPart&$tokenPart"
  }

  def percentEncode(in: String) = {
    val encoded = AuthValidator.urlEncode(in)
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

class TwitterValidator(val oauth: OAuthConf[Email])
    extends AuthValidator
    with OAuthValidator[Email] {
  val brandName = "Twitter"
  val requestTokenUrl = FullUrl.https("api.twitter.com", "/oauth/request_token")
  val accessTokenUrl = FullUrl.https("api.twitter.com", "/oauth/access_token")
  val userInfoUrl = FullUrl.https("api.twitter.com", "/1.1/account/verify_credentials.json")

  def authTokenUrl(token: RequestToken) =
    FullUrl("https", "api.twitter.com", s"/oauth/authenticate?oauth_token=$token")

  def start(req: RequestHeader, extraParams: Map[String, String] = Map.empty): Future[Result] =
    fetchRequestToken(FullUrls(redirCall, req)).map { r =>
      r.filter(_.oauthCallbackConfirmed)
        .map { tokens =>
          Redirect(authTokenUrl(tokens.oauthToken).url)
            .addingToSession(RequestToken.Key -> tokens.oauthToken.token)(req)
        }
        .getOrElse {
          handler.onUnauthorized(OAuthError("Callback not confirmed."), req)
        }
    }

  def validateCallback(req: RequestHeader): Future[Result] = {
    val maybe = for {
      token <- req.getQueryString(OauthTokenKey)
      if req.session.get(RequestToken.Key).contains(token)
      verifier <- req.getQueryString(OauthVerifierKey)
    } yield {
      fetchAccessToken(RequestToken(token), verifier).flatMap { maybeAccess =>
        maybeAccess
          .map { access =>
            fetchUser(access)
              .map { user =>
                handler.resultFor(user.email.toRight(OAuthError("Email missing.")), req)
              }
              .recover {
                case re: ResponseException =>
                  handler.onUnauthorized(OkError(re.error), req)
              }
          }
          .getOrElse {
            handler.onUnauthorizedFut(OAuthError("No access token in response."), req)
          }
      }
    }
    maybe.getOrElse(handler.onUnauthorizedFut(OAuthError("Invalid callback parameters."), req))
  }

  private def fetchRequestToken(redirUrl: FullUrl): Future[Option[TwitterTokens]] = {
    val encodable = Encodable(buildNonce, Map("oauth_callback" -> redirUrl.url))
    val authHeaderValue = encodable.signed("POST", requestTokenUrl, None)
    http
      .postForm(requestTokenUrl, form = Map.empty, headers = Map(AUTHORIZATION -> authHeaderValue))
      .map { r =>
        TwitterTokens.fromString(r.asString)
      }
  }

  private def fetchAccessToken(requestToken: RequestToken,
                               verifier: String): Future[Option[TwitterAccess]] = {
    val encodable = paramsStringWith(requestToken, buildNonce)
    val authHeaderValue = encodable.signed("POST", accessTokenUrl, None)
    http
      .postForm(
        accessTokenUrl,
        form = Map(OauthVerifierKey -> verifier),
        headers = Map(
          AUTHORIZATION -> authHeaderValue,
          CONTENT_TYPE -> MimeTypes.FORM
        )
      )
      .map { res =>
        TwitterAccess.fromString(res.asString)
      }
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
      new Request.Builder().url(reqUrl.url).addHeader(AUTHORIZATION, authHeaderValue).get.build()
    http.execute(req).flatMap { res =>
      res
        .parse[TwitterUser]
        .fold(err => Future.failed(com.malliina.http.JsonError(err, res, reqUrl).toException),
              user => Future.successful(user))
    }
  }

  private def buildNonce =
    new String(
      Base64.getEncoder.encode(CodeValidator.randomString().getBytes(StandardCharsets.UTF_8)),
      StandardCharsets.UTF_8)

  private def paramsStringWith(token: TokenValue,
                               nonce: String,
                               map: Map[String, String] = Map.empty) =
    Encodable(nonce, Map(OauthTokenKey -> token.token) ++ map)

  case class Encodable(nonce: String, map: Map[String, String]) {
    private val params = map ++ Map(
      "oauth_consumer_key" -> clientConf.clientId,
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
      val key = signingKey(clientConf.clientSecret, oauthTokenSecret)
      val signature = sign(key, signatureBaseString)
      val headerParams = encodedParams ++ Map("oauth_signature" -> percentEncode(signature))
      val authHeaderValues = headerParams.map { case (k, v) => s"""$k="$v"""" }.mkString(", ")
      s"OAuth $authHeaderValues"
    }
  }

}
