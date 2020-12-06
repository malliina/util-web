package com.malliina.web

import com.malliina.http.{FullUrl, OkClient}
import com.malliina.web.CognitoAuthFlow.{IdentityProviderKey, staticConf}
import com.malliina.web.OAuthKeys.{
  AuthorizationCode,
  ClientIdKey,
  CodeKey,
  GrantType,
  RedirectUri,
  ResponseType
}

import scala.concurrent.{ExecutionContext, Future}

trait GenericAuthConf {
  def conf: AuthConf
  def http: OkClient
}

object CognitoAuthFlow {
  val IdentityProviderKey = "identity_provider"

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
}

class CognitoAuthFlow(
  host: String,
  identityProvider: IdentityProvider,
  validator: CognitoIdValidator,
  val oauth: GenericAuthConf
) extends CallbackValidator[CognitoUser]
  with StaticFlowStart {
  val clientConf = oauth.conf
  override val conf: StaticConf = staticConf(host, oauth.conf)
  implicit val ec: ExecutionContext = oauth.http.exec

  override def validate(
    code: Code,
    redirectUrl: FullUrl,
    requestNonce: Option[String]
  ): Future[Either[AuthError, CognitoUser]] = {
    val params = tokenParameters(code, redirectUrl)
    for {
      tokens <- oauth.http.postFormAs[CognitoTokens](conf.tokenEndpoint, params)
    } yield validator.validate(tokens.idToken)
  }

  def tokenParameters(code: Code, redirUrl: FullUrl): Map[String, String] = Map(
    GrantType -> AuthorizationCode,
    ClientIdKey -> clientConf.clientId.value,
    CodeKey -> code.code,
    RedirectUri -> redirUrl.url
  )

  override def extraRedirParams(redirectUrl: FullUrl): Map[String, String] = Map(
    IdentityProviderKey -> identityProvider.name,
    ResponseType -> CodeKey
  )
}
