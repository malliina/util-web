package com.malliina.web

import cats.effect.IO
import com.malliina.http.{FullUrl, HttpClient}
import com.malliina.util.AppLogger
import com.malliina.values.Email
import com.malliina.web.GitHubAuthFlow.{staticConf, log}
import com.malliina.web.WebHeaders.Accept

object GitHubAuthFlow {
  private val log = AppLogger(getClass)

  def staticConf(conf: AuthConf) = StaticConf(
    "user:email",
    FullUrl.https("github.com", "/login/oauth/authorize"),
    FullUrl.https("github.com", "/login/oauth/access_token"),
    conf
  )
}
class GitHubAuthFlow(authConf: AuthConf, http: HttpClient[IO])
  extends StaticFlowStart
  with CallbackValidator[Email] {
  override val conf: StaticConf = staticConf(authConf)

  override def validate(
    code: Code,
    redirectUrl: FullUrl,
    requestNonce: Option[String]
  ): IO[Either[AuthError, Email]] = {
    val headers = Map(Accept -> HttpConstants.Json)
    val postHeaders = Map(WebHeaders.ContentType -> HttpConstants.FormUrlEncoded) ++ headers
    val params = validationParams(code, redirectUrl, authConf)
    for {
      tokens <- http.postFormAs[GitHubTokens](conf.tokenEndpoint, params, postHeaders)
      emails <- http.getAs[Seq[GitHubEmail]](
        FullUrl.https("api.github.com", s"/user/emails"),
        headers ++ Map(WebHeaders.Authorization -> s"token ${tokens.accessToken}")
      )
    } yield emails
      .find(email => email.primary && email.verified)
      .map(_.email)
      .toRight(JsonError("No primary and verified email found."))
  }
}
