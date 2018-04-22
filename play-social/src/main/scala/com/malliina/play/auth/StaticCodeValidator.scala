package com.malliina.play.auth

import com.malliina.http.FullUrl
import com.malliina.play.auth.CodeValidator.{ClientId, RedirectUri, Scope, State}
import com.malliina.play.auth.StaticCodeValidator.StaticConf
import com.malliina.play.http.FullUrls
import play.api.mvc.Results.Redirect
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

object StaticCodeValidator {

  case class StaticConf(scope: String,
                        authorizationEndpoint: FullUrl,
                        tokenEndpoint: FullUrl,
                        authConf: AuthConf) {
    def clientId = authConf.clientId

    def clientSecret = authConf.clientSecret
  }

  object StaticConf {
    def facebook(authConf: AuthConf) = StaticConf(
      "public_profile email",
      FullUrl.https("www.facebook.com", "/v2.12/dialog/oauth"),
      FullUrl.https("graph.facebook.com", "/v2.12/oauth/access_token"),
      authConf
    )

    def github(authConf: AuthConf) = StaticConf(
      "user:email",
      FullUrl.https("github.com", "/login/oauth/authorize"),
      FullUrl.https("github.com", "/login/oauth/access_token"),
      authConf
    )
  }

}

/** A validator where the authorization and token endpoints are static,
  * that is, no discovery endpoint is used.
  *
  * @param brandName  provider name
  * @param staticConf conf
  */
abstract class StaticCodeValidator[U](val brandName: String, val staticConf: StaticConf)
  extends CodeValidator[U] {

  override def conf = staticConf.authConf

  override def start(req: RequestHeader): Future[Result] = {
    val state = randomState()
    val params = Map(
      ClientId -> conf.clientId,
      RedirectUri -> FullUrls(redirCall, req).url,
      State -> state,
      Scope -> staticConf.scope
    ) ++ extraRedirParams(req)
    val encodedParams = params.mapValues(urlEncode)
    val url = staticConf.authorizationEndpoint.append(s"?${stringify(encodedParams)}")
    fut(Redirect(url.url).withSession(State -> state))
  }

  def extraRedirParams(rh: RequestHeader): Map[String, String] = Map.empty
}
