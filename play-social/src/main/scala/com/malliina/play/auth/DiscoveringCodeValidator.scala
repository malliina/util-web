package com.malliina.play.auth

import com.malliina.play.auth.DiscoveringCodeValidator.log
import com.malliina.play.http.FullUrls
import com.malliina.web._
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Results.BadGateway
import play.api.mvc.{Call, RequestHeader, Result}

import scala.concurrent.Future

object DiscoveringCodeValidator {
  private val log = Logger(getClass)
}

/** A validator where the authorization and token endpoints are obtained through a discovery endpoint ("knownUrl").
  *
  * @param codeConf conf
  * @tparam V type of authenticated user
  */
abstract class DiscoveringCodeValidator[V](
  codeConf: AuthCodeConf,
  results: AuthResults[V],
  val redirCall: Call
) extends DiscoveringAuthFlow[V](codeConf)
  with PlaySupport[Verified]
  with AuthValidator {

  override def onOutcome(outcome: Either[AuthError, Verified], req: RequestHeader): Result =
    results.resultFor(outcome.flatMap(parse), req)

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
}

abstract class StandardOAuth[V](conf: CodeValidationConf[V])
  extends DiscoveringCodeValidator[V](conf.codeConf, conf.handler, conf.redirCall)
  with LoginHintSupport
