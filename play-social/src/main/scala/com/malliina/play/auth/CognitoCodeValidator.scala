package com.malliina.play.auth

import com.malliina.http.{FullUrl, OkClient}
import com.malliina.play.auth.CodeValidator._
import com.malliina.play.auth.CognitoCodeValidator.IdentityProvider
import com.malliina.play.auth.StaticCodeValidator.StaticConf
import com.malliina.play.http.FullUrls
import play.api.mvc.{Call, RequestHeader}

import scala.concurrent.Future

object CognitoCodeValidator {
  val IdentityProvider = "identity_provider"

  val IdentityAmazon = "LoginWithAmazon"
  val IdentityFacebook = "Facebook"
  val IdentityGoogle = "Google"

  /**
    * @param host e.g. myapp.auth.eu-west-1.amazoncognito.com
    * @return conf
    */
  def conf(host: String, authConf: AuthConf) = StaticConf(
    "aws.cognito.signin.user.admin email openid phone profile",
    FullUrl.https(host, "/oauth2/authorize"),
    FullUrl.https(host, "/oauth2/token"),
    authConf
  )
}

class CognitoCodeValidator(identityProvider: String,
                           validator: CognitoIdValidator,
                           val redirCall: Call,
                           val handler: AuthHandlerBase[CognitoUser],
                           staticConf: StaticConf,
                           val http: OkClient)
  extends StaticCodeValidator[CognitoUser]("Amazon", staticConf) {

  override def validate(code: Code, req: RequestHeader): Future[Either[AuthError, CognitoUser]] = {
    val params = tokenParameters(code, FullUrls(redirCall, req))
    postForm[CognitoTokens](staticConf.tokenEndpoint, params).mapRight { tokens =>
      validator.validate(tokens.idToken)
    }
  }

  def tokenParameters(code: Code, redirUrl: FullUrl): Map[String, String] = Map(
    GrantType -> AuthorizationCode,
    ClientId -> conf.clientId,
    CodeKey -> code.code,
    RedirectUri -> redirUrl.url
  )

  override def extraRedirParams(rh: RequestHeader): Map[String, String] = Map(
    IdentityProvider -> identityProvider,
    ResponseType -> CodeKey
  )
}
