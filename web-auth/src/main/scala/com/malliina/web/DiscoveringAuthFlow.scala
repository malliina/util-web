package com.malliina.web

import com.malliina.http.FullUrl
import com.malliina.values.{ErrorMessage, IdToken}
import com.malliina.web.OAuthKeys._
import com.malliina.web.Utils.randomString

import scala.concurrent.{ExecutionContext, Future}

abstract class DiscoveringAuthFlow[V](codeConf: AuthCodeConf) extends AuthFlow[Future, Verified] {
  val brandName = codeConf.brandName
  val client = codeConf.client
  val conf = codeConf.conf
  val http = codeConf.client.http
  implicit val ec: ExecutionContext = http.exec

  def parse(v: Verified): Either[JWTError, V]

  override def start(redirectUrl: FullUrl, extraParams: Map[String, String]): Future[Start] =
    fetchConf().map { oauthConf =>
      val nonce = randomString()
      val params = commonAuthParams(scope, redirectUrl, conf.clientId) ++
        Map(ResponseType -> CodeKey, Nonce -> nonce) ++
        codeConf.extraStartParams ++
        extraParams
      Start(oauthConf.authorizationEndpoint, params, Option(nonce))
    }

  override def validate(
    code: Code,
    redirectUrl: FullUrl,
    requestNonce: Option[String]
  ): Future[Either[AuthError, Verified]] = {
    val params = validationParams(code, redirectUrl, conf) ++
      Map(GrantType -> AuthorizationCode) ++
      codeConf.extraValidateParams
    for {
      oauthConf <- fetchConf()
      tokens <- http.postFormAs[SimpleTokens](oauthConf.tokenEndpoint, params)
      result <- client.validate(tokens.idToken)
    } yield {
      for {
        verified <- result
        _ <- checkNonce(tokens.idToken, verified, requestNonce)
      } yield verified
    }
  }

  def checkNonce(
    idToken: IdToken,
    verified: Verified,
    requestNonce: Option[String]
  ): Either[JWTError, Verified] =
    verified.parsed.readString(Nonce).flatMap { n =>
      if (requestNonce.contains(n)) Right(verified)
      else Left(InvalidClaims(idToken, ErrorMessage("Nonce mismatch.")))
    }

  def fetchConf(): Future[AuthEndpoints] = http.getAs[AuthEndpoints](client.knownUrl)
}
