package com.malliina.web

import com.malliina.http.{FullUrl, OkClient}
import com.malliina.values.Email
import com.malliina.web.GitHubAuthFlow.staticConf
import com.malliina.web.WebHeaders.Accept

import scala.concurrent.{ExecutionContext, Future}

object GitHubAuthFlow {
  def staticConf(conf: AuthConf) = StaticConf(
    "user:email",
    FullUrl.https("github.com", "/login/oauth/authorize"),
    FullUrl.https("github.com", "/login/oauth/access_token"),
    conf
  )
}
class GitHubAuthFlow(authConf: AuthConf, http: OkClient)
  extends StaticFlowStart
  with CallbackValidator[Email] {
  override val conf: StaticConf = staticConf(authConf)
  implicit val ec: ExecutionContext = http.exec

  override def validate(
    code: Code,
    redirectUrl: FullUrl,
    requestNonce: Option[String]
  ): Future[Either[AuthError, Email]] = {
    val headers = Map(Accept -> HttpConstants.Json)
    val params = validationParams(code, redirectUrl, authConf)
    for {
      tokens <- http.postFormAs[GitHubTokens](conf.tokenEndpoint, headers, params)
      tokenUrl = FullUrl.https("api.github.com", s"/user/emails?access_token=${tokens.accessToken}")
      emails <- http.getAs[Seq[GitHubEmail]](tokenUrl)
    } yield emails
      .find(email => email.primary && email.verified)
      .map(_.email)
      .toRight(JsonError("No primary and verified email found."))
  }
}
