package com.malliina.play.auth

import com.malliina.http.OkClient
import com.malliina.values.Email
import com.malliina.web.{AuthConf, AuthError, ClientId, ClientSecret}
import play.api.mvc._

import scala.concurrent.Future

class ReadmeSamples extends munit.FunSuite {
  val http = OkClient.default
  val credentials = AuthConf(ClientId("client_id_here"), ClientSecret("client_secret_here"))
  lazy val callback: Call = ???
  val handler: AuthResults[Email] = new AuthResults[Email] {
    override def onAuthenticated(user: Email, req: RequestHeader): Result = ???

    override def onUnauthorized(error: AuthError, req: RequestHeader): Result = ???
  }
  import http.exec

  test("samples".ignore) {
    val google = GoogleCodeValidator(OAuthConf(callback, handler, credentials, http))
    val facebook = FacebookCodeValidator(OAuthConf(callback, handler, credentials, http))
    val microsoft = MicrosoftCodeValidator(OAuthConf(callback, handler, credentials, http))
    val twitter = TwitterValidator(OAuthConf(callback, handler, credentials, http))
    val github = GitHubCodeValidator(OAuthConf(callback, handler, credentials, http))

    def startGoogle = Action.async { req => google.start(req) }

    def callbackGoogle = Action.async { req => google.validateCallback(req) }
  }

  object Action {
    def async(req: RequestHeader => Future[Result]): Action[AnyContent] = ???
  }

}
