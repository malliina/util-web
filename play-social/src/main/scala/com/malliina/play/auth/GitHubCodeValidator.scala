package com.malliina.play.auth

import com.malliina.http.{FullUrl, OkClient}
import com.malliina.play.auth.StaticCodeValidator.StaticConf
import com.malliina.values.Email
import play.api.http.{HeaderNames, MimeTypes}
import play.api.mvc.{Call, RequestHeader, Result}

import scala.concurrent.Future

class GitHubCodeValidator(val redirCall: Call,
                          val handler: AuthHandler,
                          conf: AuthConf,
                          val http: OkClient)
  extends StaticCodeValidator[Email]("GitHub", StaticConf.github(conf)) with HandlerLike {

  override def validate(code: Code, req: RequestHeader): Future[Either[AuthError, Email]] = {
    val headers = Map(HeaderNames.ACCEPT -> MimeTypes.JSON)
    val params = validationParams(code, req)

    def tokenUrl(token: AccessToken) =
      FullUrl.https("api.github.com", s"/user/emails?access_token=$token")

    postEmpty[GitHubTokens](staticConf.tokenEndpoint, headers, params).flatMapRight { tokens =>
      getJson[Seq[GitHubEmail]](tokenUrl(tokens.accessToken)).mapRight { emails =>
        emails.find(email => email.primary && email.verified).map { primaryEmail =>
          Right(primaryEmail.email)
        }.getOrElse {
          Left(JsonError("No primary and verified email found."))
        }
      }
    }
  }
}

trait HandlerLike {
  self: CodeValidator[Email] =>

  def handler: AuthHandler

  override def onOutcome(outcome: Either[AuthError, Email], req: RequestHeader): Result =
    handler.resultFor(outcome, req)
}
