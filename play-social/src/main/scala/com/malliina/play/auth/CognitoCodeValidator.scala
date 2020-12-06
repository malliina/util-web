package com.malliina.play.auth

import com.malliina.play.http.FullUrls
import com.malliina.web._
import play.api.mvc.{Call, RequestHeader, Result}

import scala.concurrent.Future

object CognitoCodeValidator {
  def apply(
    host: String,
    identityProvider: IdentityProvider,
    validator: CognitoIdValidator,
    oauth: OAuthConf[CognitoUser]
  ) = new CognitoCodeValidator(host, identityProvider, validator, oauth)
}

class CognitoCodeValidator(
  host: String,
  identityProvider: IdentityProvider,
  validator: CognitoIdValidator,
  oauth: OAuthConf[CognitoUser]
) extends CognitoAuthFlow(host, identityProvider, validator, oauth)
  with PlaySupport[CognitoUser] {
  def redirCall: Call = oauth.redirCall

  override def onOutcome(outcome: Either[AuthError, CognitoUser], req: RequestHeader): Result =
    oauth.handler.resultFor(outcome, req)

  def validate(code: Code, req: RequestHeader): Future[Either[AuthError, CognitoUser]] =
    validate(code, FullUrls(redirCall, req), None)
}
