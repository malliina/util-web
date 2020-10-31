package com.malliina.play.auth

import com.malliina.http.FullUrl
import com.malliina.play.auth.AuthValidator.Start
import com.malliina.play.auth.DiscoveringCodeValidator.log
import com.malliina.play.auth.OAuthKeys._
import com.malliina.play.http.FullUrls
import com.malliina.values.{ErrorMessage, IdToken}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Results.BadGateway
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

object DiscoveringCodeValidator {
  private val log = Logger(getClass)
}

/** A validator where the authorization and token endpoints are obtained through
  * a discovery endpoint ("knownUrl").
  *
  * @param codeConf conf
  * @tparam V type of authenticated user
  */
abstract class DiscoveringCodeValidator[V](codeConf: AuthCodeConf)
  extends CodeValidator[Verified, V] {

  val brandName = codeConf.brandName
  val client = codeConf.client

  def parse(v: Verified): Either[JWTError, V]

  override def onOutcome(outcome: Either[AuthError, Verified], req: RequestHeader): Result =
    handler.resultFor(outcome.flatMap(parse), req)

  /** The initial result that initiates sign-in.
    */
  override def start(
    req: RequestHeader,
    extraParams: Map[String, String] = Map.empty
  ): Future[Result] =
    start(FullUrls(redirCall, req), extraParams)
      .map { s => redirResult(s.authorizationEndpoint, s.params, s.nonce) }
      .recover {
        case e =>
          log.error(s"HTTP error.", e)
          BadGateway(Json.obj("message" -> "HTTP error."))
      }

  override def start(redirectUrl: FullUrl, extraParams: Map[String, String]): Future[Start] =
    fetchConf().map { oauthConf =>
      val nonce = randomString()
      val params = commonAuthParams(scope, redirectUrl) ++
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
    val params = validationParams(code, redirectUrl) ++
      Map(GrantType -> AuthorizationCode) ++
      codeConf.extraValidateParams
    for {
      oauthConf <- fetchConf()
      tokens <- postForm[SimpleTokens](oauthConf.tokenEndpoint, params)
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

  def fetchConf(): Future[AuthEndpoints] = getJson[AuthEndpoints](client.knownUrl)
}

abstract class StandardOAuth[V](conf: CodeValidationConf[V])
  extends DiscoveringCodeValidator[V](conf.codeConf)
  with LoginHintSupport {
  override val oauth: OAuthConf[V] = conf.oauth
}
