package com.malliina.play.auth

import com.malliina.http.FullUrl
import com.malliina.play.auth.CodeValidator._
import com.malliina.play.auth.CognitoCodeValidator.{IdentityProvider, IdentityProviderKey, staticConf}
import com.malliina.play.auth.StaticCodeValidator.StaticConf
import com.malliina.play.http.FullUrls
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

object CognitoCodeValidator {
  val IdentityProviderKey = "identity_provider"

  def apply(host: String,
            identityProvider: IdentityProvider,
            validator: CognitoIdValidator,
            oauth: OAuthConf[CognitoUser]) =
    new CognitoCodeValidator(host, identityProvider, validator, oauth)

  /**
    * @param host e.g. myapp.auth.eu-west-1.amazoncognito.com
    * @return conf
    */
  def staticConf(host: String, authConf: AuthConf) = StaticConf(
    "aws.cognito.signin.user.admin email openid phone profile",
    FullUrl.https(host, "/oauth2/authorize"),
    FullUrl.https(host, "/oauth2/token"),
    authConf
  )

  sealed abstract class IdentityProvider(val name: String)

  object IdentityProvider {

    case object LoginWithAmazon extends IdentityProvider("LoginWithAmazon")

    case object IdentityFacebook extends IdentityProvider("Facebook")

    case object IdentityGoogle extends IdentityProvider("Google")

    case class IdentityOther(n: String) extends IdentityProvider(n)

  }

}

class CognitoCodeValidator(host: String,
                           identityProvider: IdentityProvider,
                           validator: CognitoIdValidator,
                           val oauth: OAuthConf[CognitoUser])
  extends StaticCodeValidator[CognitoUser, CognitoUser]("Amazon", staticConf(host, oauth.conf)) {

  override def onOutcome(outcome: Either[AuthError, CognitoUser], req: RequestHeader): Result =
    handler.resultFor(outcome, req)

  override def validate(code: Code, req: RequestHeader): Future[Either[AuthError, CognitoUser]] = {
    val params = tokenParameters(code, FullUrls(redirCall, req))
    postForm[CognitoTokens](staticConf.tokenEndpoint, params).mapRight { tokens =>
      validator.validate(tokens.idToken)
    }
  }

  def tokenParameters(code: Code, redirUrl: FullUrl): Map[String, String] = Map(
    GrantType -> AuthorizationCode,
    ClientId -> clientConf.clientId,
    CodeKey -> code.code,
    RedirectUri -> redirUrl.url
  )

  override def extraRedirParams(rh: RequestHeader): Map[String, String] = Map(
    IdentityProviderKey -> identityProvider.name,
    ResponseType -> CodeKey
  )
}
