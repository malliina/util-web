package com.malliina.play.auth

import com.malliina.http.{FullUrl, OkClient}
import com.malliina.play.auth.StaticCodeValidator.StaticConf
import com.malliina.play.models.Email
import play.api.http.{HeaderNames, MimeTypes}
import play.api.mvc.{Call, RequestHeader}

import scala.concurrent.Future

class GitHubCodeValidator(val redirCall: Call, val handler: AuthHandler, val conf: AuthConf, val http: OkClient)
  extends StaticCodeValidator("GitHub", StaticConf.github(conf)) {

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
