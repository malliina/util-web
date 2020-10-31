package com.malliina.play.auth

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import com.malliina.http.FullUrl
import com.malliina.play.auth.AuthValidator.Start
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

object AuthValidator {
  def urlEncode(s: String) = URLEncoder.encode(s, StandardCharsets.UTF_8.name())

  case class Start(
    authorizationEndpoint: FullUrl,
    params: Map[String, String],
    nonce: Option[String]
  )

  case class Callback(
    requestState: Option[String],
    sessionState: Option[String],
    codeQuery: Option[String],
    requestNonce: Option[String],
    redirectUrl: FullUrl
  )
}

trait AuthValidator {
  def brandName: String

  /** The initial result that initiates sign-in.
    */
  def start(req: RequestHeader, extraParams: Map[String, String] = Map.empty): Future[Result]
  def start(redirectUrl: FullUrl, extraParams: Map[String, String]): Future[Start]

  /** The callback in the auth flow, i.e. the result for redirect URIs.
    */
  def validateCallback(req: RequestHeader): Future[Result]

  protected def stringify(map: Map[String, String]): String =
    map.map { case (key, value) => s"$key=$value" }.mkString("&")

  protected def fut[T](t: T): Future[T] = Future.successful(t)
}

trait LoginHintSupport { self: AuthValidator =>
  def startHinted(
    req: RequestHeader,
    loginHint: Option[String],
    extraParams: Map[String, String] = Map.empty
  ): Future[Result] =
    self.start(
      req,
      extraParams ++ loginHint.map(lh => Map(OAuthKeys.LoginHint -> lh)).getOrElse(Map.empty)
    )

  def startHinted(
    redirectUrl: FullUrl,
    loginHint: Option[String],
    extraParams: Map[String, String]
  ): Future[Start] =
    self.start(
      redirectUrl,
      extraParams ++ loginHint.map(lh => Map(OAuthKeys.LoginHint -> lh)).getOrElse(Map.empty)
    )
}

trait OAuthValidator[U] {
  implicit def exec: ExecutionContext = http.exec
  def oauth: OAuthConf[U]
  def handler = oauth.handler
  def redirCall = oauth.redirCall
  def http = oauth.http
  def clientConf = oauth.conf
}
