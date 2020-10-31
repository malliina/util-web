package com.malliina.play.auth

import com.malliina.http.FullUrl
import com.malliina.play.auth.StaticCodeValidator.StaticConf
import com.malliina.values.Email
import play.api.http.{HeaderNames, MimeTypes}
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

object GitHubCodeValidator {
  def apply(conf: OAuthConf[Email]) = new GitHubCodeValidator(conf)

  def staticConf(conf: AuthConf) = StaticConf(
    "user:email",
    FullUrl.https("github.com", "/login/oauth/authorize"),
    FullUrl.https("github.com", "/login/oauth/access_token"),
    conf
  )
}

class GitHubCodeValidator(val oauth: OAuthConf[Email])
  extends StaticCodeValidator[Email, Email]("GitHub", StaticConf.github(oauth.conf))
  with HandlerLike {

  override def validate(
    code: Code,
    redirectUrl: FullUrl,
    requestNonce: Option[String]
  ): Future[Either[AuthError, Email]] = {
    val headers = Map(HeaderNames.ACCEPT -> MimeTypes.JSON)
    val params = validationParams(code, redirectUrl)

    for {
      tokens <- postEmpty[GitHubTokens](staticConf.tokenEndpoint, headers, params)
      tokenUrl = FullUrl.https("api.github.com", s"/user/emails?access_token=${tokens.accessToken}")
      emails <- getJson[Seq[GitHubEmail]](tokenUrl)
    } yield emails
      .find(email => email.primary && email.verified)
      .map(_.email)
      .toRight(JsonError("No primary and verified email found."))
  }
}

trait HandlerLike {
  self: CodeValidator[Email, Email] =>

  def handler: AuthResults[Email]

  override def onOutcome(outcome: Either[AuthError, Email], req: RequestHeader): Result =
    handler.resultFor(outcome, req)
}
