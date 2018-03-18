package com.malliina.play.auth

import com.malliina.http.FullUrl
import com.malliina.play.auth.CodeValidator.{ClientId, RedirectUri, Scope, State}
import com.malliina.play.auth.StaticCodeValidator.StaticConf
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
      FullUrl("https", "www.facebook.com", "/v2.12/dialog/oauth"),
      FullUrl("https", "graph.facebook.com", s"/v2.12/oauth/access_token"),
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

abstract class StaticCodeValidator(val brandName: String, val staticConf: StaticConf)
  extends CodeValidator {

  override def start(req: RequestHeader): Future[Result] = {
    val state = randomState()
    val params = Map(
      ClientId -> conf.clientId,
      RedirectUri -> redirUrl(redirCall, req),
      State -> state,
      Scope -> staticConf.scope
    )
    val url = staticConf.authorizationEndpoint.append(s"?${stringify(params)}")
    fut(Redirect(url.url).withSession(State -> state))
  }
}
