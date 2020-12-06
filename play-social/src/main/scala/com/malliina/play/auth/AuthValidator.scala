package com.malliina.play.auth

import com.malliina.web.OAuthKeys.LoginHint
import com.malliina.web.{FlowStart, LoginHint}
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future}

trait AuthValidator extends FlowStart[Future] {
  def brandName: String

  /** The initial result that initiates sign-in.
    */
  def start(req: RequestHeader, extraParams: Map[String, String] = Map.empty): Future[Result]

  /** The callback in the auth flow, i.e. the result for redirect URIs.
    */
  def validateCallback(req: RequestHeader)(implicit ec: ExecutionContext): Future[Result]
}

trait LoginHintSupport extends LoginHint[Future] { self: AuthValidator =>
  def startHinted(
    req: RequestHeader,
    loginHint: Option[String],
    extraParams: Map[String, String] = Map.empty
  ): Future[Result] = self.start(
    req,
    extraParams ++ loginHint.map(lh => Map(LoginHint -> lh)).getOrElse(Map.empty)
  )
}

trait OAuthValidator[U] {
  def oauth: OAuthConf[U]
  def handler = oauth.handler
  def redirCall = oauth.redirCall
  def http = oauth.http
  def clientConf = oauth.conf
}
