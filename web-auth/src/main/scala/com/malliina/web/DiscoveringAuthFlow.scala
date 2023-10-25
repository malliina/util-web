package com.malliina.web

import cats.effect.Sync
import cats.syntax.all.{toFlatMapOps, toFunctorOps}
import com.malliina.http.{FullUrl, HttpClient}
import com.malliina.values.{ErrorMessage, IdToken}
import com.malliina.web.OAuthKeys.*
import com.malliina.web.Utils.randomString

abstract class DiscoveringAuthFlow[F[_]: Sync, V](codeConf: AuthCodeConf[F])
  extends AuthFlow[F, Verified]:
  val brandName: String = codeConf.brandName
  val client: KeyClient[F] = codeConf.client
  val conf: AuthConf = codeConf.conf
  val http: HttpClient[F] = codeConf.client.http

  def parse(v: Verified): Either[JWTError, V]

  override def start(redirectUrl: FullUrl, extraParams: Map[String, String]): F[Start] =
    fetchConf().map: oauthConf =>
      val nonce = randomString()
      val params = commonAuthParams(scope, redirectUrl, conf.clientId) ++
        Map(ResponseType -> CodeKey, Nonce -> nonce) ++
        codeConf.extraStartParams ++
        extraParams
      Start(oauthConf.authorizationEndpoint, params, Option(nonce))

  override def validate(
    code: Code,
    redirectUrl: FullUrl,
    requestNonce: Option[String]
  ): F[Either[AuthError, Verified]] =
    val params = validationParams(code, redirectUrl, conf) ++
      Map(GrantType -> AuthorizationCode) ++
      codeConf.extraValidateParams
    for
      oauthConf <- fetchConf()
      tokens <- http.postFormAs[SimpleTokens](oauthConf.tokenEndpoint, params)
      result <- client.validate(tokens.idToken)
    yield for
      verified <- result
      _ <- checkNonce(tokens.idToken, verified, requestNonce)
    yield verified

  def checkNonce(
    idToken: IdToken,
    verified: Verified,
    requestNonce: Option[String]
  ): Either[JWTError, Verified] =
    verified.parsed
      .readString(Nonce)
      .flatMap: n =>
        if requestNonce.contains(n) then Right(verified)
        else Left(InvalidClaims(idToken, ErrorMessage("Nonce mismatch.")))

  def fetchConf(): F[AuthEndpoints] = http.getAs[AuthEndpoints](client.knownUrl)
