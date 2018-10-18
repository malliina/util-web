package com.malliina.play.auth

import com.malliina.play.auth.CodeValidator._
import com.malliina.play.auth.DiscoveringCodeValidator.log
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

  def parse(v: Verified): Either[AuthError, V]

  override def onOutcome(outcome: Either[AuthError, Verified], req: RequestHeader): Result =
    handler.resultFor(outcome.flatMap(parse), req)

  /** The initial result that initiates sign-in.
    */
  override def start(req: RequestHeader, extraParams: Map[String, String] = Map.empty): Future[Result] =
    fetchConf().mapR { oauthConf =>
      val nonce = randomString()
      val params = commonAuthParams(scope, req) ++
        Map(ResponseType -> CodeKey, Nonce -> nonce) ++
        codeConf.extraStartParams ++
        extraParams
      redirResult(oauthConf.authorizationEndpoint, params, Option(nonce))
    }.onFail { err =>
      log.error(s"HTTP error. $err")
      BadGateway(Json.obj("message" -> "HTTP error."))
    }

  override def validate(code: Code, req: RequestHeader): Future[Either[AuthError, Verified]] = {
    val params = validationParams(code, req) ++
      Map(GrantType -> AuthorizationCode) ++
      codeConf.extraValidateParams
    fetchConf().flatMapRight { oauthConf =>
      postForm[SimpleTokens](oauthConf.tokenEndpoint, params).flatMapRight { tokens =>
        client.validate(tokens.idToken).map { result =>
          for {
            verified <- result
            _ <- checkNonce(tokens.idToken, verified, req)
          } yield verified
        }
      }
    }
  }

  def checkNonce(idToken: IdToken, verified: Verified, req: RequestHeader): Either[JWTError, Verified] =
    verified.parsed.readString(Nonce).flatMap { n =>
      if (req.session.get(Nonce).contains(n)) Right(verified)
      else Left(InvalidClaims(idToken, "Nonce mismatch."))
    }

  def fetchConf(): Future[Either[OkError, AuthEndpoints]] =
    getJson[AuthEndpoints](client.knownUrl)
}

abstract class StandardOAuth[V](conf: CodeValidationConf[V])
  extends DiscoveringCodeValidator[V](conf.codeConf)
    with LoginHintSupport {
  override val oauth: OAuthConf[V] = conf.oauth
}
