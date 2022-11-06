package com.malliina.web

import cats.effect.Sync
import cats.syntax.all.*
import com.malliina.http.{FullUrl, HttpClient}
import com.malliina.web.CognitoAuthFlow.{IdentityProviderKey, staticConf}
import com.malliina.web.OAuthKeys.*

trait GenericAuthConf[F[_]]:
  def conf: AuthConf
  def http: HttpClient[F]

object CognitoAuthFlow:
  val IdentityProviderKey = "identity_provider"

  /** @param host
    *   e.g. myapp.auth.eu-west-1.amazoncognito.com
    * @return
    *   conf
    */
  def staticConf(host: String, authConf: AuthConf) = StaticConf(
    "aws.cognito.signin.user.admin email openid phone profile",
    FullUrl.https(host, "/oauth2/authorize"),
    FullUrl.https(host, "/oauth2/token"),
    authConf
  )

class CognitoAuthFlow[F[_]: Sync](
  host: String,
  identityProvider: IdentityProvider,
  validator: CognitoIdValidator,
  val oauth: GenericAuthConf[F]
) extends CallbackValidator[F, CognitoUser]
  with StaticFlowStart[F]:
  val clientConf = oauth.conf
  override val conf: StaticConf = staticConf(host, oauth.conf)

  override def validate(
    code: Code,
    redirectUrl: FullUrl,
    requestNonce: Option[String]
  ): F[Either[AuthError, CognitoUser]] =
    val params = tokenParameters(code, redirectUrl)
    for tokens <- oauth.http.postFormAs[CognitoTokens](conf.tokenEndpoint, params)
    yield validator.validate(tokens.idToken)

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
