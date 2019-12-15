package com.malliina.play.auth

import com.malliina.http.FullUrl
import com.malliina.play.auth.StaticCodeValidator.StaticConf
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

object StaticCodeValidator {

  case class StaticConf(
    scope: String,
    authorizationEndpoint: FullUrl,
    tokenEndpoint: FullUrl,
    authConf: AuthConf
  ) {
    def clientId = authConf.clientId

    def clientSecret = authConf.clientSecret
  }

  object StaticConf {
    def facebook(authConf: AuthConf) =
      FacebookCodeValidator.staticConf(authConf)

    def github(authConf: AuthConf) =
      GitHubCodeValidator.staticConf(authConf)
  }

}

/** A validator where the authorization and token endpoints are static,
  * that is, no discovery endpoint is used.
  *
  * @param brandName  provider name
  * @param staticConf conf
  */
abstract class StaticCodeValidator[U, V](val brandName: String, val staticConf: StaticConf)
  extends CodeValidator[U, V] {

  override def start(
    req: RequestHeader,
    extraParams: Map[String, String] = Map.empty
  ): Future[Result] = {
    val params = commonAuthParams(staticConf.scope, req) ++ extraRedirParams(req) ++ extraParams
    fut(redirResult(staticConf.authorizationEndpoint, params))
  }

  def extraRedirParams(rh: RequestHeader): Map[String, String] = Map.empty
}
